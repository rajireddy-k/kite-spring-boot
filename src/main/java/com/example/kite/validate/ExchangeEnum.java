package com.example.kite.validate;

public enum ExchangeEnum {
    BSE("BSE"),
    NSE("NSE"),
    NSEFO("NSEFO"),
    NSECD("NSECD"),
    MCX("MCX"),
    MCXFO("MCXFO"),
    CDS("CDS"),
    BFO("BFO");

    private final String exchange;

    ExchangeEnum(String exchange) {
        this.exchange = exchange;
    }

    public String getOrderType() {
        return exchange;
    }

}
