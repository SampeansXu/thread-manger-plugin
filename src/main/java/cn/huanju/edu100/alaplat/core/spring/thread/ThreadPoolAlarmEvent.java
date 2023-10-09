package cn.huanju.edu100.alaplat.core.spring.thread;

import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * @author hexiangtao
 * @date 2022/10/10 12:38
 */
public class ThreadPoolAlarmEvent extends ApplicationEvent {

    private final List<String> messages;


    public ThreadPoolAlarmEvent(Object source, List<String> messages) {
        super(source);
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return "TheadPoolAlarmEvent [messages=" + this.messages + ",source=" + this.source + ']';
    }
}
