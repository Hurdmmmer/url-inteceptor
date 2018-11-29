package com.tdh.urlInterceptor.enums;

public enum LimitEnum {
    /** 区分用户 */
    DISTINGUISH_USERS(0),
    /** 不区分用户 */
    DO_NOT_DISTINGUISH__USERS(1);

    private int value;

    LimitEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
