//package cn.huanju.edu100.alaplat.core.spring.thread.config;
//
//import cn.huanju.edu100.alaplat.core.spring.thread.ThreadPoolScheduler;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//
///**
// * @Description: 线程池控制器 自动配置
// * @Author: xushengbin@hqwx.com
// * @Date: 2021-12-16
// */
//@Configuration
////@EnableConfigurationProperties(ThreadPoolSchedulerProperties.class)
//public class ThreadPoolSchedulerAutoConfig {
//    @Bean
//    @ConditionalOnMissingBean(ThreadPoolScheduler.class)
//    public ThreadPoolScheduler threadPoolScheduler(ThreadPoolSchedulerProperties config) {
//        return new ThreadPoolScheduler(config);
//    }
//
//}
