package com.example.kite.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class TradingViewSignal {

    private String secret;
    private String source;
    private String symbol;
    private String marketSymbol;
    private String action;
    private Integer quantity;
    private Double price;
    private Double open;
    private Double high;
    private Double low;
    private Double volume;

}
