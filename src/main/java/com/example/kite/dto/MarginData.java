package com.example.kite.dto;


import lombok.Data;


import java.math.BigDecimal;


@Data
public class MarginData {


    private String type;
    private String tradingsymbol;
    private String exchange;
    private BigDecimal span;
    private BigDecimal exposure;
    private BigDecimal option_premium;
    private BigDecimal additional;
    private BigDecimal bo;
    private BigDecimal cash;
    private BigDecimal var;
    private BigDecimal mtf;
    private Pnl pnl;
    private BigDecimal leverage;
    private Charges charges;
    private BigDecimal total;
}
