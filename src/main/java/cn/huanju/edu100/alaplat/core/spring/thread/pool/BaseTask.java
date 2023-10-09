package cn.huanju.edu100.alaplat.core.spring.thread.pool;

/**
 * @Description: 任务基础类
 * @Author: xushengbin@hqwx.com
 * @Date: 2021-12-21
 */
public abstract class BaseTask implements Cloneable {
    private long createTime = 0;
    private long startTime = 0;
    private String name;
    private TaskType type;

    protected abstract void run();

    protected BaseTask(String name, TaskType type) {
        this.createTime = System.currentTimeMillis();
        this.name = name;
        this.type = type;
    }

    @Override
    public BaseTask clone() {
        BaseTask task = null;

        try {
            task = (BaseTask)super.clone();
        } catch (CloneNotSupportedException var3) {
            var3.printStackTrace();
        }

        return task;
    }

    @Override
    public String toString() {
        long now = System.currentTimeMillis();

        return "Task[" +
                "name:" + this.name +
                ", type:" + this.type.toString() +
                ", DelayTime:" + this.toDelayTime(now) +
                ", RunTime:" + this.toRunTime(now) +
                ", createTime:" + this.createTime +
                ", startTime:" + this.startTime +
                ']';
    }

    public long getCreateTime() {
        return this.createTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public String getName() {
        return this.name;
    }

    public TaskType getType() {
        return this.type;
    }

    private String formatTime(long millisecond) {
        String result = "";

        long s = millisecond / 1000;
        long days = s / (60 * 60 * 24);
        long hours = (s % (60 * 60 * 24)) / (60 * 60);
        long minutes = (s % (60 * 60)) / 60;
        long seconds = s % 60;
        long milSecond = millisecond % 1000;

        final String Separate = "";
        if (days > 0) {
            result += days + "d" + Separate;
        }
        if (hours > 0) {
            result += hours + "h" + Separate;
        }
        if (minutes > 0) {
            result += minutes + "m" + Separate;
        }
        if (seconds > 0) {
            result += seconds + "s" + Separate;
        }
        if (milSecond >= 0) {
            result += milSecond + "ms";
        }

        result = result + "(T:" + millisecond + "ms)";
        return result;
    }

    public String toDelayTime(long now) {
        long delayTime = this.startTime - this.createTime;
        if (this.startTime <= 0) {
            delayTime = now - this.createTime;
        }

        return formatTime(delayTime);
    }

    public String toRunTime(long now) {
        long runTime = now - this.startTime;
        if (this.startTime <= 0) {
            runTime = 0;
        }

        return formatTime(runTime);
    }
}
