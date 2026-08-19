package com.example.kite.trading.util;


import com.example.kite.trading.dto.Candle;


public final class CandleValidator {
    private CandleValidator() {}


    public static void validate(Candle candle) {
        if (candle == null) throw new IllegalArgumentException("Candle cannot be null");
        if (candle.getSymbol() == null || candle.getSymbol().isBlank()) {
            throw new IllegalArgumentException("Candle symbol is required");
        }
        if (candle.getTimestamp() == null) throw new IllegalArgumentException("Candle timestamp is required");


        double open = candle.getOpen();
        double high = candle.getHigh();
        double low = candle.getLow();
        double close = candle.getClose();
        double volume = candle.getVolume();


        if (!Double.isFinite(open) || !Double.isFinite(high) ||
                !Double.isFinite(low) || !Double.isFinite(close) ||
                !Double.isFinite(volume)) {
            throw new IllegalArgumentException("Candle OHLCV values must be finite");
        }
        if (high < low) throw new IllegalArgumentException("Candle high cannot be below low");
        if (open < low || open > high) throw new IllegalArgumentException("Open outside candle range");
        if (close < low || close > high) throw new IllegalArgumentException("Close outside candle range");
        if (volume < 0) throw new IllegalArgumentException("Volume cannot be negative");
    }
}