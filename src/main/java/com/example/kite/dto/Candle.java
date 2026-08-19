package com.example.kite.dto;

import lombok.Data;

/*
 * =============================================================
 * CANDLE
 * =============================================================
 */
@Data
public class Candle {

    private final long timestamp;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double volume;

}
