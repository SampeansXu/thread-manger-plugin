package cn.huanju.edu100.alaplat.core.spring.thread.pool;

import cn.huanju.edu100.alaplat.core.spring.thread.ThreadPoolScheduler;
import cn.huanju.edu100.alaplat.core.spring.thread.config.ThreadPoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Description: 线程池
 * @Author: xushengbin@hqwx.com
 * @Date: 2021-12-13
 */
public abstract class ThreadPoolInner {

    protected static final Logger logger = LoggerFactory.getLogger(ThreadPoolScheduler.class);

    protected ReentrantLock mainLock = new ReentrantLock();
    protected Map<String, ThreadPool> poolMap = new HashMap<>();

    // 外部接口 /////////////////////////////////////////////////////////
    public abstract void execute(String poolName, ThreadTask task);

    public abstract <T> ThreadFuture<T> submit(String poolName, ThreadFutureTask<T> task);


    // 内部调用 /////////////////////////////////////////////////////////
    protected void createPools(List<ThreadPoolProperties> threadPools) {
        this.mainLock.lock();
        for (ThreadPoolProperties tpp : threadPools) {
            ThreadPool threadPool = new ThreadPool(
                    tpp.getPoolSize(),
                    tpp.getName(),
                    tpp.getAlarmTaskTime(),
                    tpp.getAlarmTaskCount());

            Assert.isTrue(!this.poolMap.containsKey(tpp.getName()), "Config ThreadPool is repeated. PoolName:" + tpp.getName());
            this.poolMap.put(tpp.getName(), threadPool);
        }
        this.mainLock.unlock();
    }

    protected void addTask(String poolName, BaseTask task) {
        this.mainLock.lock();
        try {
            if (CollectionUtils.isEmpty(this.poolMap)) {
                this.mainLock.unlock();
                throw new NullPointerException("Do not exist any ThreadPool");
            }

            ThreadPool pool = this.poolMap.get(poolName);
            if (Objects.isNull(pool)) {
                this.mainLock.unlock();
                throw new NullPointerException("The ThreadPool:" + poolName + " is not exist");
            }

            pool.execute(task);
        } finally {

        }
        this.mainLock.unlock();
    }

    protected void shutdown() {
        this.mainLock.lock();
        try {
            if (!CollectionUtils.isEmpty(this.poolMap)) {
                for (ThreadPool pool : this.poolMap.values()) {
                    if (Objects.nonNull(pool)) {
                        pool.shutdown();
                    }
                }
            }
        } finally {

        }
        this.mainLock.unlock();
    }

    protected List<String> checkAlarm() {
        List<String> result = new LinkedList<>();

        this.mainLock.lock();
        try {
            if (!CollectionUtils.isEmpty(this.poolMap)) {
                for (ThreadPool pool : this.poolMap.values()) {
                    if (Objects.nonNull(pool)) {
                        List<String> alarms = pool.needAlarms();
                        if (!CollectionUtils.isEmpty(alarms)) {
                            result.addAll(alarms);
                        }
                    }
                }
            }
        } finally {

        }
        this.mainLock.unlock();

        return result;
    }

    protected void printPoolsInfo() {
        this.mainLock.lock();
        try {
            if (!CollectionUtils.isEmpty(this.poolMap)) {
                for (ThreadPool pool : this.poolMap.values()) {
                    if (Objects.nonNull(pool)) {
                        if (Objects.nonNull(logger)) {
                            logger.info("[ThreadPoolScheduler] {}", pool);
                        }
                    }
                }
            }
        } finally {

        }
        this.mainLock.unlock();
    }

    /****************************************************************************************
     * 内部线程池
     */
    private class ThreadPool {
        private int poolSize;
        private String poolName;
        private Long taskAlarmTime;
        private Integer taskAlarmCount;

        private final RuntimePermission shutdownPerm = new RuntimePermission("changeThread");
        private final ReentrantLock poolLock = new ReentrantLock();
        private LinkedBlockingQueue<BaseTask> taskQueue = new LinkedBlockingQueue<>();
        private final List<ThreadExecutor> threadQueue = new ArrayList<>();
        private int lastThreadIdx = -1;

        public ThreadPool(int poolSize, String name) {
            this(poolSize, name, null, null);
        }

        public ThreadPool(int poolSize, String name, Long taskAlarmTime, Integer taskAlarmCount) {
            Assert.isTrue(poolSize > 0, "Constructor failed(poolSize invalid. must poolSize > 0)");
            Assert.hasText(name, "Constructor failed(name is blank)");

            this.poolSize = poolSize;
            this.poolName = name;
            if (Objects.nonNull(taskAlarmTime)) {
                this.taskAlarmTime = taskAlarmTime * 1000;
            }
            this.taskAlarmCount = taskAlarmCount;
            Assert.isTrue(init(), "Constructor failed(ThreadPool init failed)");
        }

        public void execute(BaseTask task) {
            this.taskQueue.offer(task);

            this.poolLock.lock();
            try {
                for (int i = 0; i < this.poolSize; i++) {
                    this.lastThreadIdx = (this.lastThreadIdx + 1) % this.poolSize;
                    ThreadExecutor executor = this.threadQueue.get(this.lastThreadIdx);
                    if (ThreadState.Idle.getCode() == executor.getThreadState().getCode()) {
                        executor.awaken();
                        break;
                    }
                }
            } finally {
            }
            this.poolLock.unlock();
        }

        public void shutdown() {
            this.checkAccessThreads();
            this.interruptThreads();
        }

        @Override
        public String toString() {
            int workThread = 0, idleThread = 0;
            this.poolLock.lock();
            {
                for (ThreadExecutor executor : this.threadQueue) {
                    if (ThreadState.Idle.getCode() == executor.getThreadState().getCode()) {
                        idleThread += 1;
                    } else if (ThreadState.Working.getCode() == executor.getThreadState().getCode()
                            && executor.isLocked()) {
                        workThread += 1;
                    }
                }
            }
            this.poolLock.unlock();

            return "ThreadPool[" +
                    "name:" + this.poolName +
                    ", totalThread:" + this.poolSize +
                    ", workThread:" + workThread +
                    ", idleThread:" + idleThread +
                    ", taskQueue:" + this.taskQueue.size() +
                    ']';
        }

        public List<String> needAlarms() {
            List<String> result = new LinkedList<>();
            String alarmCount = alarmTaskCount();
            if (StringUtils.hasText(alarmCount)) {
                result.add(alarmCount);
            }

            if (Objects.isNull(this.taskAlarmTime) || this.taskAlarmTime <= 0) {
                return result;
            }
            this.poolLock.lock();
            {
                for (ThreadExecutor executor : this.threadQueue) {
                    String alarm = executor.needAlarm();
                    if (StringUtils.hasText(alarm)) {
                        result.add(alarm);
                    }
                }
            }
            this.poolLock.unlock();

            return result;
        }

        private String alarmTaskCount() {
            if (Objects.isNull(this.taskAlarmCount) || this.taskAlarmCount <= 0) {
                return null;
            }

            final int count = this.taskQueue.size();
            if (count < this.taskAlarmCount) {
                return null;
            }

            String result = "Pool(" + this.poolName +
                    ")'s taskQueue size(" + count + ") has above " + this.taskAlarmCount +
                    "";
            return result;
        }

        private boolean init() {
            this.poolLock.lock();
            for (int i = 0; i < this.poolSize; i++) {
                ThreadExecutor executor = new ThreadExecutor(i);
                this.threadQueue.add(executor);
            }
            this.poolLock.unlock();

            return true;
        }

        private void checkAccessThreads() {
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkPermission(shutdownPerm);
                this.poolLock.lock();
                try {
                    for (ThreadExecutor executor : this.threadQueue) {
                        security.checkAccess(executor.thread);
                    }
                } finally {
                }
                this.poolLock.unlock();
            }
        }

        private void interruptThreads() {
            this.poolLock.lock();
            try {
                for (ThreadExecutor executor : this.threadQueue) {
                    executor.stop();
                }
            } finally {
            }
            this.poolLock.unlock();
        }

        private BaseTask getTask() {
            BaseTask task = null;
            try {
                task = this.taskQueue.take();
            } catch (InterruptedException e) {
            }

            return task;
        }

        final void handleTask(ThreadExecutor executor) {
            Thread currThread = Thread.currentThread();
            executor.unlock();

            while (this.taskQueue.size() > 0) {
                if (!executor.isRunning) {
                    if (Thread.interrupted() && !currThread.isInterrupted()) {
                        currThread.interrupt();
                    }
                    break;
                }

                executor.lock();
                {
                    final BaseTask task = this.getTask();
                    if (Objects.nonNull(task)) {
                        executor.setTask(task);
                        try {
                            executor.getTask().run();

                            if (Objects.nonNull(logger)) {
                                logger.info("[ThreadPool] ThreadPool.handleTask(Task:{}) finished. {}", task.getName(), executor.toString());
                            }

                        } catch (Exception | Error e) {
                            ThreadPoolScheduler.ReportAlarm.I().report(executor.alarmException(e));
                            if (Objects.nonNull(logger)) {
                                logger.warn("[ThreadPool] ThreadPool.handleTask(Task:{}) failed. {}", task.getName(), executor.toString(), e);
                            }
                        } finally {
                            executor.setTask(null);
                        }

                        final String alarm = executor.checkTaskOver(task);
                        if (StringUtils.hasText(alarm)) {
                            ThreadPoolScheduler.ReportAlarm.I().report(alarm);
                        }
                    }
                }
                executor.unlock();
            }
        }

        final class ThreadExecutor extends AbstractQueuedSynchronizer implements Runnable {
            int id;

            Thread thread;
            boolean isRunning;
            ThreadState state = ThreadState.Idle;
            long lastCheckTime = 0;

            final ReentrantLock threadLock = new ReentrantLock();
            BaseTask currTask;

            public ThreadExecutor(int id) {
                this.id = id;
                this.thread = new Thread(this, poolName + "-Thread-" + this.id);
                this.isRunning = true;
                this.thread.start();
            }

            @Override
            protected boolean isHeldExclusively() {
                return this.getState() != 0;
            }

            @Override
            protected boolean tryAcquire(int unused) {
                if (this.compareAndSetState(0, 1)) {
                    this.setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
                return false;
            }

            @Override
            protected boolean tryRelease(int unused) {
                this.setExclusiveOwnerThread(null);
                this.setState(0);
                return true;
            }

            public void lock() {
                this.acquire(1);
            }

            public boolean tryLock() {
                return this.tryAcquire(1);
            }

            public void unlock() {
                this.release(1);
            }

            public boolean isLocked() {
                return this.isHeldExclusively();
            }

            void interrupt() {
                Thread t;
                if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    }
                }
            }


            @Override
            public void run() {
                this.state = ThreadState.Idle;

                while (this.isRunning) {
                    LockSupport.park();

                    try {
                        if (!this.isRunning || this.thread.isInterrupted()) {
                            break;
                        }
                        this.state = ThreadState.Working;
                        this.lastCheckTime = System.currentTimeMillis();

                        handleTask(this);

                    } catch (Exception | Error ignored) {
                    } finally {
                        this.state = ThreadState.Idle;
                    }
                }

                this.lastCheckTime = 0;
                this.state = ThreadState.Stopped;
            }

            @Override
            public String toString() {
                String result = "ThreadExecutor[ThreadPool:" + poolName +
                        ", Thread:[name:" + this.thread.getName() + ", state:" + this.state.toString() + "]";
                final BaseTask task = this.getTask();
                if (Objects.nonNull(task)) {
                    long now = System.currentTimeMillis();
                    result += ", Task:[name:" + task.getName() +
                            ", type:" + task.getType().toString() +
                            ", DelayTime:" + task.toDelayTime(now) +
                            ", RunTime:" + task.toRunTime(now) +
                            "]";
                } else {
                    result += ", Task:null";
                }
                result += "]";
                return result;
            }

            public String needAlarm() {
                if (Objects.isNull(taskAlarmTime) || taskAlarmTime <= 0) {
                    return null;
                }

                final BaseTask task = this.getTask();
                if (ThreadState.Idle.getCode() == this.state.getCode()
                        || Objects.isNull(task)) {
                    return null;
                }

                long now = System.currentTimeMillis();
                if (task.getStartTime() <= 0
                        || (now - task.getStartTime()) < taskAlarmTime
                        || (now - this.lastCheckTime) < taskAlarmTime) {
                    return null;
                }
                this.lastCheckTime = System.currentTimeMillis();

                String result = "Task(name:" + task.getName() + ", type:" + task.getType().getExplain() + ")" +
                        " executing long time, runTime:(" + task.toRunTime(now) + "). " +
                        "Pool:" + poolName + ", Thread:" + this.thread.getName() +
                        "";
                return result;
            }

            public String alarmException(Throwable e) {
                if (Objects.isNull(e)) {
                    return null;
                } else {
                    String result = "Thread:" + this.thread.getName();
                    BaseTask task = this.getTaskClone();
                    if (Objects.nonNull(task)) {
                        long now = System.currentTimeMillis();
                        result = result + ", Task(name:" + task.getName() + ", type:" + task.getType().getExplain() + ", RunTime:" + task.toRunTime(now) + ")";
                    } else {
                        result = result + ", Task:null";
                    }

                    result = result + ". ThreadExecutor exception: " + e.getMessage();
                    return result;
                }
            }

            public String checkTaskOver(final BaseTask task) {
                if (Objects.isNull(taskAlarmTime) || taskAlarmTime <= 0
                        || Objects.isNull(task)) {
                    return null;
                }

                long now = System.currentTimeMillis();
                if (task.getStartTime() <= 0
                        || (now - task.getStartTime()) < taskAlarmTime
                        || (now - this.lastCheckTime) < taskAlarmTime) {
                    return null;
                }
                this.lastCheckTime = System.currentTimeMillis();

                String result = "Task(name:" + task.getName() + ", type:" + task.getType().getExplain() + ")" +
                        " executed finished overtime, UsedTime:(" + task.toRunTime(now) + "). " +
                        "Pool:" + poolName + ", Thread:" + this.thread.getName() +
                        "";
                return result;
            }

            public ThreadState getThreadState() {
                return this.state;
            }

            public void awaken() {
                LockSupport.unpark(this.thread);
            }

            public void stop() {
                this.state = ThreadState.Stopping;
                this.isRunning = false;
                this.awaken();
                //TODO 不强制中断线程
                //this.interrupt();
            }

            public void setTask(BaseTask task) {
                this.threadLock.lock();
                this.currTask = task;
                if (Objects.nonNull(this.lastCheckTime)) {
                    this.lastCheckTime = System.currentTimeMillis();
                }
                this.threadLock.unlock();
            }

            public final BaseTask getTask() {
                this.threadLock.lock();
                BaseTask task = this.currTask;
                this.threadLock.unlock();

                return task;
            }

            public final BaseTask getTaskClone() {
                this.threadLock.lock();
                BaseTask task = null;
                if (Objects.nonNull(this.currTask)) {
                    task = this.currTask.clone();
                }

                this.threadLock.unlock();
                return task;
            }
        }
    }
}
