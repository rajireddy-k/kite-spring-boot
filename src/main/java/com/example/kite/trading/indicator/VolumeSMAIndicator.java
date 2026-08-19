package com.example.kite.trading.indicator;


public final class VolumeSMAIndicator {
    private final int period;
    private final double[] window;
    private int size;
    private int cursor;
    private double sum;


    public VolumeSMAIndicator(int period) {
        if (period <= 0) throw new IllegalArgumentException("Volume SMA period must be > 0");
        this.period = period;
        this.window = new double[period];
    }


    public double update(double volume) {
        if (!Double.isFinite(volume) || volume < 0) {
            throw new IllegalArgumentException("Volume must be finite and >= 0");
        }


        if (size < period) {
            window[cursor] = volume;
            sum += volume;
            size++;
        } else {
            sum -= window[cursor];
            window[cursor] = volume;
            sum += volume;
        }


        cursor = (cursor + 1) % period;
        return sum / size;
    }


    public boolean isReady() {
        return size >= period;
    }


    public double getValue() {
        if (!isReady()) throw new IllegalStateException("Volume SMA is not warmed up");
        return sum / period;
    }
}
