package cn.huanju.edu100.alaplat.core.spring.thread.pool;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description: 同步线程任务
 * @Author: xushengbin@hqwx.com
 * @Date: 2021-12-21
 */
public class ThreadFutureTask<T> extends BaseTask implements ThreadFuture<T> {
    private Callable<T> target;
    private boolean isDone;
    private Object backValue = null;

    private ReentrantLock lock = new ReentrantLock();
    private volatile CountDownLatch sycLatch = new CountDownLatch(1);
    private volatile Thread currentThread;

    public ThreadFutureTask(Callable<T> target, String name) {
        super(name, TaskType.Future);

        if (Objects.isNull(target)) {
            throw new IllegalArgumentException("target is null");
        }
        this.target = target;
        this.isDone = false;
    }

    @Override
    protected void run() {
        this.setStartTime(System.currentTimeMillis());

        setThread(Thread.currentThread());
        {
            T result = null;
            try {
                result = this.target.call();
                this.setDone(true);

            } catch (Exception | Error e) {
                result = null;
            } finally {
            }

            this.setBackValue(result);
            this.sycLatch.countDown();
        }
    }

    private void waitDone(long timeout, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        if (Objects.nonNull(unit)) {
            this.sycLatch.await(timeout, unit);
        } else {
            this.sycLatch.await();
        }
    }

    private void setDone(final boolean done) {
        this.lock.lock();
        this.isDone = done;
        this.lock.unlock();
    }

    private boolean getDone() {
        this.lock.lock();
        boolean result = this.isDone;
        this.lock.unlock();

        return result;
    }

    private void setThread(final Thread thread) {
        this.lock.lock();
        this.currentThread = thread;
        this.lock.unlock();
    }

    private final Thread getThread() {
        this.lock.lock();
        Thread thread = this.currentThread;
        this.lock.unlock();

        return thread;
    }

    private void setBackValue(T t) {
        this.backValue = t;
    }

    private T getBackValue() throws ExecutionException {
        if (this.getDone()) {
            return (T) this.backValue;
        }

        throw new ExecutionException((Throwable) this.backValue);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        if (!this.getDone()) {
            this.waitDone(0, null);
        }

        return this.getBackValue();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (Objects.isNull(unit) || timeout <= 0) {
            throw new NullPointerException();
        }
        if (!this.getDone()) {
            this.waitDone(timeout, unit);
        }

        if (!this.getDone()) {
            throw new TimeoutException();
        }

        return this.getBackValue();
    }

    @Override
    public void cancel() {
        final Thread thread = this.getThread();
        if (Objects.nonNull(thread)) {
            thread.interrupt();
        }
    }
}
