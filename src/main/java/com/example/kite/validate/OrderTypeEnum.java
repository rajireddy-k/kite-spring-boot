package com.example.kite.validate;

public enum OrderTypeEnum {
    MARKET("MARKET"),
    LIMIT("LIMIT"),
    SL("SL"),
    SL_LIMIT("SL-M");

    private final String orderType;

    OrderTypeEnum(String orderType) {
        this.orderType = orderType;
    }

    public String getOrderType() {
        return orderType;
    }
}
