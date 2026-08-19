package com.example.kite.trading.indicator;


public final class RSIIndicator {
    private final int period;
    private int changeCount;
    private Double previousClose;
    private double gainSum;
    private double lossSum;
    private double averageGain;
    private double averageLoss;
    private double rsi;


    public RSIIndicator(int period) {
        if (period <= 0) throw new IllegalArgumentException("RSI period must be > 0");
        this.period = period;
    }


    public double update(double close) {
        if (!Double.isFinite(close)) throw new IllegalArgumentException("Close must be finite");


        if (previousClose == null) {
            previousClose = close;
            return 0.0;
        }


        double change = close - previousClose;
        previousClose = close;


        double gain = Math.max(change, 0.0);
        double loss = Math.max(-change, 0.0);


        if (changeCount < period) {
            gainSum += gain;
            lossSum += loss;
            changeCount++;
            if (changeCount == period) {
                averageGain = gainSum / period;
                averageLoss = lossSum / period;
                rsi = calculateRsi();
            }
        } else {
            averageGain = ((averageGain * (period - 1)) + gain) / period;
            averageLoss = ((averageLoss * (period - 1)) + loss) / period;
            rsi = calculateRsi();
        }


        return rsi;
    }


    public boolean isReady() {
        return changeCount >= period;
    }


    public double getValue() {
        if (!isReady()) throw new IllegalStateException("RSI is not warmed up");
        return rsi;
    }


    private double calculateRsi() {
        if (averageLoss == 0.0) {
            return averageGain == 0.0 ? 50.0 : 100.0;
        }
        if (averageGain == 0.0) return 0.0;
        double rs = averageGain / averageLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }
}
