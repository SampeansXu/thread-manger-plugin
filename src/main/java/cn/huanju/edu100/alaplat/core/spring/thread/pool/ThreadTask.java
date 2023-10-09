package cn.huanju.edu100.alaplat.core.spring.thread.pool;

import java.util.Objects;

/**
 * @Description: 线程任务
 * @Author: xushengbin@hqwx.com
 * @Date: 2021-12-13
 */
public class ThreadTask extends BaseTask {
    private Runnable target;

    public ThreadTask(Runnable target, String name) {
        super(name, TaskType.Normal);

        if (Objects.isNull(target)) {
            throw new IllegalArgumentException("target is null");
        }
        this.target = target;
    }

    @Override
    public void run() {
        this.setStartTime(System.currentTimeMillis());

        this.target.run();
    }
}
