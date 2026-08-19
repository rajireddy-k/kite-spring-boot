package com.example.kite.trading.dto;


import lombok.Builder;
import lombok.Data;


import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
public class TradingSignal {


    private String symbol;


    private String action;


    private Double price;


    private Double entryPrice;


    private Double stopLoss;


    private int quantity;


    private Double fastEma;


    private Double slowEma;


    private String trend;


    private LocalDateTime timestamp;


    Double target;


    IndicatorSnapshot indicators;
}
