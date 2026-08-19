package com.example.kite.trading.indicator;


public final class EMAIndicator {
    private final int period;
    private final double multiplier;
    private int count;
    private double seedSum;
    private double value;


    public EMAIndicator(int period) {
        if (period <= 0) throw new IllegalArgumentException("EMA period must be > 0");
        this.period = period;
        this.multiplier = 2.0 / (period + 1.0);
    }


    public double update(double price) {
        validate(price);
        if (count < period) {
            seedSum += price;
            count++;
            if (count == period) value = seedSum / period;
        } else {
            value = ((price - value) * multiplier) + value;
            count++;
        }
        return value;
    }


    public boolean isReady() {
        return count >= period;
    }


    public double getValue() {
        if (!isReady()) throw new IllegalStateException("EMA is not warmed up");
        return value;
    }


    public int getPeriod() {
        return period;
    }


    private static void validate(double price) {
        if (!Double.isFinite(price)) throw new IllegalArgumentException("Price must be finite");
    }
}
