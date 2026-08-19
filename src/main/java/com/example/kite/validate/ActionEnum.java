package com.example.kite.validate;

public enum ActionEnum {
    BUY("BUY"),
    SELL("SELL");

    private final String action;

    ActionEnum(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
