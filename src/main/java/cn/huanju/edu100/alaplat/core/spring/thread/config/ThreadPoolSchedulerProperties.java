package cn.huanju.edu100.alaplat.core.spring.thread.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.List;

/**
 * @Description: 线程池控制器配置
 * @Author: xushengbin@hqwx.com
 * @Date: 2021-12-16
 */
@EnableConfigurationProperties(ThreadPoolSchedulerProperties.class)
@ConfigurationProperties(prefix = "hqconfig.threadpools-cheduler")
public class ThreadPoolSchedulerProperties {
    private long checkTime; //单位: 秒
    private List<ThreadPoolProperties> threadPools;

    @Override
    public String toString() {
        return "ThreadPoolSchedulerProperties{" +
                "checkTime=" + checkTime +
                ", threadPools=" + threadPools +
                '}';
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
}
