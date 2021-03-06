package com.yeahmobi.yscheduler.model.type;

/**
 * @author atell
 */
public enum AttemptStatus {

    RUNNING(1, "运行中"), SUCCESS(10, "运行成功"), FAILED(20, "运行失败"), CANCELLED(30, "取消运行"),
    COMPLETE_WITH_UNKNOWN_STATUS(40, "未知的结束状态");

    private int    id;
    private String desc;

    AttemptStatus(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public static AttemptStatus valueOf(int id) {
        switch (id) {
            case 1:
                return RUNNING;
            case 10:
                return SUCCESS;
            case 20:
                return FAILED;
            case 30:
                return CANCELLED;
            case 40:
                return COMPLETE_WITH_UNKNOWN_STATUS;
        }
        return null;
    }

    public boolean isCompleted() {
        return FAILED.equals(this) || SUCCESS.equals(this) || CANCELLED.equals(this)
               || COMPLETE_WITH_UNKNOWN_STATUS.equals(this);
    }

}
