package com.example.kite.alerts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

@Component
@Slf4j
public class IntradayTradingAlert {

    // 15 minutes × 60 seconds
    private static final long WINDOW_SECONDS = 15 * 60;

    // Avoid alerts for insignificant movements.
    // Example: 0.10 means 0.10%
    private static final double ALERT_THRESHOLD_PERCENT = 2.0;

    private final Deque<PricePoint> prices = new ArrayDeque<>();

    public synchronized Alert calculateAlert(double currentPrice) {

        long currentTime = Instant.now().getEpochSecond();

        // Add current price
        prices.addLast(new PricePoint(currentTime, currentPrice));

        // Remove prices older than 15 minutes
        long cutoffTime = currentTime - WINDOW_SECONDS;

        while (!prices.isEmpty()
                && prices.peekFirst().timestamp < cutoffTime) {
            prices.removeFirst();
        }

        // Calculate mean
        double sum = 0.0;

        for (PricePoint point : prices) {
            sum += point.price;
        }

        double mean = sum / prices.size();

        // Difference between current price and mean
        double difference = currentPrice - mean;

        // Percentage difference
        double percentageDifference =
                (difference / mean) * 100.0;

        Signal signal;

        if (percentageDifference >= ALERT_THRESHOLD_PERCENT) {
            log.info("Price is above mean by {}%, generating Sell signal", percentageDifference);
            signal = Signal.SELL;
        } else if (percentageDifference <= -ALERT_THRESHOLD_PERCENT) {
            log.info("Price is below mean by {}%, generating Buy signal", percentageDifference);
            signal = Signal.BUY;
        } else {
            log.info("Price is within {}% of mean, generating Neutral signal", ALERT_THRESHOLD_PERCENT);
            signal = Signal.NEUTRAL;
        }

        return new Alert(
                currentPrice,
                mean,
                difference,
                percentageDifference,
                signal,
                currentTime
        );
    }

    // Represents one price observation
    private static class PricePoint {

        private final long timestamp;
        private final double price;

        public PricePoint(long timestamp, double price) {
            this.timestamp = timestamp;
            this.price = price;
        }
    }

    public enum Signal {
        BUY,
        SELL,
        NEUTRAL
    }

    public static class Alert {

        private final double currentPrice;
        private final double mean;
        private final double difference;
        private final double percentageDifference;
        private final Signal signal;
        private final long timestamp;

        public Alert(
                double currentPrice,
                double mean,
                double difference,
                double percentageDifference,
                Signal signal,
                long timestamp) {

            this.currentPrice = currentPrice;
            this.mean = mean;
            this.difference = difference;
            this.percentageDifference = percentageDifference;
            this.signal = signal;
            this.timestamp = timestamp;
        }

        public double getCurrentPrice() {
            return currentPrice;
        }

        public double getMean() {
            return mean;
        }

        public double getDifference() {
            return difference;
        }

        public double getPercentageDifference() {
            return percentageDifference;
        }

        public Signal getSignal() {
            return signal;
        }

        @Override
        public String toString() {
            return String.format(
                    "Price: %.2f | Mean: %.2f | Difference: %.2f | " +
                            "Deviation: %.3f%% | Signal: %s",
                    currentPrice,
                    mean,
                    difference,
                    percentageDifference,
                    signal
            );
        }
    }
}

