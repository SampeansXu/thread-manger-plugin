package cn.huanju.edu100.alaplat.core.spring.thread.config;


/**
 * @Description: 线程池配置项
 * @Author: xushengbin@hqwx.com
 * @Date: 2021-12-16
 */
public class ThreadPoolProperties {
    private String name;
    private int poolSize;
    private Long alarmTaskTime = 10 * 1000L; //单位: 秒
    private Integer alarmTaskCount = 500;

    @Override
    public String toString() {
        return "ThreadPoolProperties{" +
                "name='" + name + '\'' +
                ", poolSize=" + poolSize +
                ", alarmTaskTime=" + alarmTaskTime +
                ", alarmTaskCount=" + alarmTaskCount +
                '}';
    }

    public int getPoolSize() {
        return this.poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAlarmTaskTime() {
        return this.alarmTaskTime;
    }

    public void setAlarmTaskTime(long alarmTaskTime) {
        this.alarmTaskTime = alarmTaskTime;
    }

    public int getAlarmTaskCount() {
        return this.alarmTaskCount;
    }

    public void setAlarmTaskCount(int alarmTaskCount) {
        this.alarmTaskCount = alarmTaskCount;
    }
}
