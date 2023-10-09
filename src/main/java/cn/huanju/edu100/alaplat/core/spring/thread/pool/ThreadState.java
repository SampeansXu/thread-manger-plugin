package cn.huanju.edu100.alaplat.core.spring.thread.pool;

/**
 * @Description: 说明
 * @Author: xushengbin@hqwx.com
 * @Date: 2021-12-13
 */
public enum ThreadState {
    //states
    Idle(0, "idle"),
    Working(1, "working"),
    Stopping(2,"stopping"),
    Stopped(3, "stopped"),
    ;

    public int getCode() {
        return code;
    }

    public String getExplain() {
        return explain;
    }

    @Override
    public String toString() {
        return code +"-" + explain;
    }

    int code;
    String explain;

    ThreadState(int code, String explain) {
        this.code = code;
        this.explain = explain;
    }
}
