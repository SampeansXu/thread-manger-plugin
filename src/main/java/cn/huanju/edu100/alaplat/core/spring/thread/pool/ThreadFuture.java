package cn.huanju.edu100.alaplat.core.spring.thread.pool;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @Description: 说明
 * @Author: xushengbin@hqwx.com
 * @Date: 2021-12-22
 */
public interface ThreadFuture<T> {
    T get() throws InterruptedException, ExecutionException;
    T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    void cancel();
}
