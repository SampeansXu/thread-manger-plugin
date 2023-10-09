package cn.huanju.edu100.alaplat.core.spring.thread.pool;

/**
 * @Description: 任务类型
 * @Author: xushengbin@hqwx.com
 * @Date: 2021-12-22
 */
public enum TaskType {
    //types
    Normal(1, "normal"),
    Future(2, "future"),
    ;

    @Override
    public String toString() {
        return this.type + "-" + this.explain;
    }

    public int getType() {
        return type;
    }

    public String getExplain() {
        return explain;
    }


    int type;
    String explain;

    TaskType(int type, String explain) {
        this.type = type;
        this.explain = explain;
    }
}
