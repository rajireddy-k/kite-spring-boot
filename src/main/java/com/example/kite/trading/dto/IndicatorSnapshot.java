package com.example.kite.trading.dto;


public record IndicatorSnapshot(
        double ema20,
        double ema50,
        double atr14,
        double rsi14,
        double vwap,
        double volumeSma20,
        double previousHigh,
        double previousLow
) {}
