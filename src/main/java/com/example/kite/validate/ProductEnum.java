package com.example.kite.validate;

public enum ProductEnum {
    CNC("CNC"),
    MIS("MIS"),
    NRML("NRML");

    private final String product;

    ProductEnum(String product) {
        this.product = product;
    }

    public String getProduct() {
        return product;
    }
}
