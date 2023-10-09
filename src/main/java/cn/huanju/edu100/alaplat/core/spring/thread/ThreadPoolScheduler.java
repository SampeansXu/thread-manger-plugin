package cn.huanju.edu100.alaplat.core.spring.thread;

import cn.huanju.edu100.alaplat.core.spring.thread.config.ThreadPoolProperties;
import cn.huanju.edu100.alaplat.core.spring.thread.config.ThreadPoolSchedulerProperties;
import cn.huanju.edu100.alaplat.core.spring.thread.pool.ThreadFuture;
import cn.huanju.edu100.alaplat.core.spring.thread.pool.ThreadFutureTask;
import cn.huanju.edu100.alaplat.core.spring.thread.pool.ThreadPoolInner;
import cn.huanju.edu100.alaplat.core.spring.thread.pool.ThreadTask;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Description: 线程池控制器
 * @Author: xushengbin@hqwx.com
 * @Date: 2021-12-13
 */
public class ThreadPoolScheduler extends ThreadPoolInner implements ApplicationContextAware {

    private ScheduledExecutorService scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();

    private long checkTime;
    private List<ThreadPoolProperties> threadPools;
    private boolean publishAlarmEvent;

    public ThreadPoolScheduler(ThreadPoolSchedulerProperties config) {
        this(config, false);
    }

    public ThreadPoolScheduler(ThreadPoolSchedulerProperties config, boolean publishAlarmEvent) {
        Assert.isTrue(Objects.nonNull(config), "[hqconfig.threadpools-cheduler] is not config");
        this.checkTime = config.getCheckTime();
        this.publishAlarmEvent = publishAlarmEvent;
        this.threadPools = config.getThreadPools();
    }

    public ThreadPoolScheduler() {
    }

    public long getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(long checkTime) {
        this.checkTime = checkTime;
    }

    public List<ThreadPoolProperties> getThreadPools() {
        return threadPools;
    }

    public void setThreadPools(List<ThreadPoolProperties> threadPools) {
        this.threadPools = threadPools;
    }

    @PostConstruct
    private void create() {
        ThreadPoolScheduler mainBean = this.applicationContext.getBean(ThreadPoolScheduler.class);
        mainBean.start();
    }

    @PreDestroy
    private void destroy() {
        System.out.println("[ThreadPoolScheduler] scheduler destroy ready...");
        if (Objects.nonNull(logger)) {
            logger.info("[ThreadPoolScheduler] scheduler destroy ready...");
        }

        this.scheduledThreadPool.shutdown();
        this.shutdown();
    }

    private void init() {
        Assert.isTrue(this.checkTime > 0, "Config checkTime is invalid.");
        Assert.isTrue(!CollectionUtils.isEmpty(this.threadPools), "Config threadPools is empty");
        this.createPools(this.threadPools);
    }

    private void start() {
        this.init();

        //定时任务
        this.scheduledThreadPool.scheduleAtFixedRate(this::handleScheduled, this.checkTime, this.checkTime, TimeUnit.SECONDS);

        printDetail();
        System.out.println("[ThreadPoolScheduler] scheduler started");
        if (Objects.nonNull(logger)) {
            logger.info("[ThreadPoolScheduler] scheduler started");
        }
    }

    private void handleScheduled() {
        this.printPoolsInfo();

        List<String> allAlarms = this.checkAlarm();
        if (CollectionUtils.isEmpty(allAlarms)) {
            return;
        }
        if (publishAlarmEvent) {
            publishAlarmEvent(allAlarms);
        }
        for (String alarm : allAlarms) {
            ReportAlarm.I().report(alarm);
        }

    }

    private void printDetail() {
        StringBuilder sb = new StringBuilder();
        final String LineSeparator = "──────────────────────────────────────────────────────────────────────────────────" + System.lineSeparator();
        sb.append(System.lineSeparator());
        sb.append("┌" + LineSeparator);
        sb.append("│ ThreadPoolScheduler Info: checkTime:" + this.checkTime + "s" + System.lineSeparator());
        {
            sb.append("├" + LineSeparator);
            sb.append("│ ThreadPools Info:" + System.lineSeparator());
            int idx = 1;
            for (ThreadPoolProperties poolConfig : this.threadPools) {
                sb.append("│ " + (idx++) + ".")
                        .append("Name:" + poolConfig.getName())
                        .append(", PoolSize:" + poolConfig.getPoolSize())
                        .append(", AlarmTaskTime:" + poolConfig.getAlarmTaskTime() + "s")
                        .append(", AlarmTaskCount:" + poolConfig.getAlarmTaskCount())
                        .append(System.lineSeparator());
            }
        }
        sb.append("└" + LineSeparator);

        System.out.println(sb.toString());
        if (Objects.nonNull(logger)) {
            logger.info(sb.toString());
        }
    }

    @Override
    public void execute(String poolName, ThreadTask task) {
        if (!StringUtils.hasText(poolName) || Objects.isNull(task)) {
            throw new IllegalArgumentException("poolName or task is null");
        }

        this.addTask(poolName, task);
    }

    @Override
    public <T> ThreadFuture<T> submit(String poolName, ThreadFutureTask<T> task) {
        if (!StringUtils.hasText(poolName) || Objects.isNull(task)) {
            throw new IllegalArgumentException("poolName or task is null");
        }

        this.addTask(poolName, task);
        return task;
    }

    private ApplicationContext applicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    /****************************************************************************************
     * 报告异常
     */
    public static class ReportAlarm {
        private static ReportAlarm inst = new ReportAlarm();

        public static ReportAlarm I() {
            return inst;
        }

        private ReportAlarm() {

        }

        private ExecutorService executors = Executors.newFixedThreadPool(2);

        public void report(final String alarm) {
            if (!StringUtils.hasText(alarm)) {
                return;
            }
            executors.execute(() -> {
                if (Objects.nonNull(logger)) {
                    logger.warn("[ThreadPoolScheduler] report alarm:{}", alarm);
                }

                throw new RuntimeException(alarm);
            });
        }
    }

    private void publishAlarmEvent(List<String> message) {
        try {
            applicationContext.publishEvent(new ThreadPoolAlarmEvent(this, message));
        } catch (Exception ex) {
            //do nothing
            logger.warn("publishAlarmEvent failed,message:{}", message, ex);
        }
    }
}
