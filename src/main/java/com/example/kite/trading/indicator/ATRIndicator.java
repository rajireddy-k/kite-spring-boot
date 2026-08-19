package com.example.kite.trading.indicator;


public final class ATRIndicator {
    private final int period;
    private int trCount;
    private Double previousClose;
    private double trSum;
    private double atr;


    public ATRIndicator(int period) {
        if (period <= 0) throw new IllegalArgumentException("ATR period must be > 0");
        this.period = period;
    }


    public double update(double high, double low, double close) {
        validate(high, low, close);
        if (high < low) throw new IllegalArgumentException("High cannot be below low");


        double trueRange = previousClose == null
                ? high - low
                : Math.max(high - low,
                Math.max(Math.abs(high - previousClose), Math.abs(low - previousClose)));


        previousClose = close;


        if (trCount < period) {
            trSum += trueRange;
            trCount++;
            if (trCount == period) atr = trSum / period;
        } else {
            atr = ((atr * (period - 1)) + trueRange) / period;
            trCount++;
        }
        return atr;
    }


    public boolean isReady() {
        return trCount >= period;
    }


    public double getValue() {
        if (!isReady()) throw new IllegalStateException("ATR is not warmed up");
        return atr;
    }


    private static void validate(double high, double low, double close) {
        if (!Double.isFinite(high) || !Double.isFinite(low) || !Double.isFinite(close)) {
            throw new IllegalArgumentException("OHLC values must be finite");
        }
    }
}
