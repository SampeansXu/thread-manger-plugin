#使用说明
##1.pom.xml 引入
```
<dependency>
    <groupId>cn.huanju.edu100</groupId>
    <artifactId>alaplat-core-spring</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```
##2.项目创建线程池
####2.1 Spring Boot 配置文件方式
项目配置文件 yml格式
```editorconfig
hqconfig:
  threadpools-cheduler:
    checkTime: 10  #线程池定时检查时间，单位：秒
    threadPools:   #线程池组
      - name: fastPool       #线程池名称
        poolSize: 1000       #线程池大小
        alarmTaskTime: 10    #线程池任务执行告警时间，单位：秒
        alarmTaskCount: 100  #线程池任务数告警
      - name: slowPool
        poolSize: 300
        alarmTaskTime: 20
        alarmTaskCount: 500
```
项目配置类
```java
@Configuration
public class ThreadPoolSchedulerConfig {
    @Bean
    public ThreadPoolSchedulerProperties threadPoolsConfig() {
        return  new ThreadPoolSchedulerProperties();
    }

    @Bean
    public ThreadPoolScheduler threadPoolScheduler(ThreadPoolSchedulerProperties threadPoolsConfig) {
        return new ThreadPoolScheduler(poolsConfig);
    }
}
```
####2.2 Spring Boot Bean配置方式
xml配置
```xml
<!-- 线程池 Bean -->
<bean id="fastPool" class="cn.huanju.edu100.alaplat.core.spring.thread.config.ThreadPoolProperties">
    <property name="name" value="fastPool"/>
    <property name="poolSize" value="1000"/>
    <property name="alarmTaskTime" value="10"/>
    <property name="alarmTaskCount" value="100"/>
</bean>
<!-- 线程池 Bean -->
<bean id="slowPool" class="cn.huanju.edu100.alaplat.core.spring.thread.config.ThreadPoolProperties">
    <property name="name" value="slowPool"/>
    <property name="poolSize" value="300"/>
    <property name="alarmTaskTime" value="20"/>
    <property name="alarmTaskCount" value="500"/>
</bean>

<!-- 线程池控制器 Bean -->
<bean id="threadPoolScheduler" class="cn.huanju.edu100.alaplat.core.spring.thread.ThreadPoolScheduler">
    <property name="checkTime" value="10"/>
    <property name="threadPools">
        <list>
            <ref bean="fastPool"/>
            <ref bean="slowPool"/>
        </list>
    </property>
</bean>
```
##3.业务服务调用
```java
@Resource
ThreadPoolScheduler threadPoolScheduler;

public void executeTask(Object param) {
    final String poolName = "fastPool";
    threadPoolScheduler.execute(poolName, new ThreadTask(new Runnable() {
        @Override
        public void run() {
            System.out.println("---> executeTask.run " + param);
        }
    }, "taskName-" + param));
}
```

#编译提交
```
mvn clean package deploy -Dmaven.test.skip=true
```