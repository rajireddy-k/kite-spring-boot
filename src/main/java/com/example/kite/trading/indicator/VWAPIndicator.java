package com.example.kite.trading.indicator;


import java.time.LocalDate;


public final class VWAPIndicator {
    private double cumulativePriceVolume;
    private double cumulativeVolume;
    private LocalDate sessionDate;


    public double update(LocalDate date, double high, double low, double close, double volume,
                         boolean resetOnSessionChange) {
        if (date == null) throw new IllegalArgumentException("Date cannot be null");
        if (!Double.isFinite(high) || !Double.isFinite(low) || !Double.isFinite(close) ||
                !Double.isFinite(volume) || volume < 0) {
            throw new IllegalArgumentException("Invalid VWAP input");
        }
        if (high < low) throw new IllegalArgumentException("High cannot be below low");


        if (resetOnSessionChange && sessionDate != null && !sessionDate.equals(date)) {
            cumulativePriceVolume = 0.0;
            cumulativeVolume = 0.0;
        }
        sessionDate = date;


        double typicalPrice = (high + low + close) / 3.0;
        cumulativePriceVolume += typicalPrice * volume;
        cumulativeVolume += volume;


        return cumulativeVolume == 0.0 ? typicalPrice : cumulativePriceVolume / cumulativeVolume;
    }


    public double getValue() {
        if (cumulativeVolume == 0.0) throw new IllegalStateException("VWAP has no volume");
        return cumulativePriceVolume / cumulativeVolume;
    }
}
