package cn.huanju.edu100.alaplat.core.spring.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @Description: Spring Context工具
 * @Author: xushengbin@hqwx.com
 * @Date: 2021-05-15
 */
@Component("SDK_alaplat_core_spring_SpringContextUtil")
@Order(Integer.MIN_VALUE)
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext context = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    // 获取Bean
    public static <T> T getBean(String beanName) {
        if(Objects.isNull(context)){
            return null;
        }

        return (T) context.getBean(beanName);
    }
    public static <T> T getBean(Class<T> clasz) {
        if(Objects.isNull(context)){
            return null;
        }

        return (T) context.getBean(clasz);
    }

    // 获取当前环境
    public static String getActiveProfile() {
        if(Objects.isNull(context)){
            return null;
        }

        return context.getEnvironment().getActiveProfiles()[0];
    }
}
