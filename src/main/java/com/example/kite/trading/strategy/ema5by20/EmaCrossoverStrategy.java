package com.example.kite.trading.strategy.ema5by20;


import com.example.kite.trading.dto.Candle;
import com.example.kite.trading.dto.TradingSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.lang.Double;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;


@Slf4j
@Component
public class EmaCrossoverStrategy {


    private static final int FAST_LENGTH = 5;
    private static final int SLOW_LENGTH = 20;
    private static final int ORDER_QUANTITY = 50;


    /**
     * Evaluate the latest CLOSED candle.
     *
     * Pine equivalent:
     *
     * buySignal  = ta.crossover(fastEMA, slowEMA)
     * sellSignal = ta.crossunder(fastEMA, slowEMA)
     */
    public TradingSignal evaluate(
            String symbol,
            List<Candle> candles) {


        if (candles == null || candles.size() < SLOW_LENGTH + 1) {
            log.debug(
                    "Not enough candles for EMA calculation. Symbol={}, candles={}",
                    symbol,
                    candles == null ? 0 : candles.size()
            );


            return null;
        }


        // -------------------------------------------------
        // IMPORTANT:
        // candles MUST be in chronological order:
        //
        // OLD -> NEW
        //
        // candles[0] = oldest
        // candles[last] = latest CLOSED candle
        // -------------------------------------------------


        List<Double> closes = candles.stream()
                .map(Candle::getClose)
                .toList();


        // Current EMA values
        Double currentFastEMA =
                calculateEMA(closes, FAST_LENGTH);


        Double currentSlowEMA =
                calculateEMA(closes, SLOW_LENGTH);


        // Previous candle EMA values
        List<Double> previousCloses =
                closes.subList(0, closes.size() - 1);


        Double previousFastEMA =
                calculateEMA(previousCloses, FAST_LENGTH);


        Double previousSlowEMA =
                calculateEMA(previousCloses, SLOW_LENGTH);


        log.debug(
                "EMA calculation symbol={}, previousFast={}, previousSlow={}, " +
                        "currentFast={}, currentSlow={}",
                symbol,
                previousFastEMA,
                previousSlowEMA,
                currentFastEMA,
                currentSlowEMA
        );


        // -------------------------------------------------
        // TradingView:
        //
        // ta.crossover(fastEMA, slowEMA)
        //
        // Current fast > current slow
        // AND
        // Previous fast <= previous slow
        // -------------------------------------------------


        boolean buySignal =
                currentFastEMA.compareTo(currentSlowEMA) > 0
                        &&
                        previousFastEMA.compareTo(previousSlowEMA) <= 0;


        // -------------------------------------------------
        // TradingView:
        //
        // ta.crossunder(fastEMA, slowEMA)
        //
        // Current fast < current slow
        // AND
        // Previous fast >= previous slow
        // -------------------------------------------------


        boolean sellSignal =
                currentFastEMA.compareTo(currentSlowEMA) < 0
                        &&
                        previousFastEMA.compareTo(previousSlowEMA) >= 0;


        // -------------------------------------------------
        // Active trend
        // -------------------------------------------------


        String trend;


        if (currentFastEMA.compareTo(currentSlowEMA) > 0) {
            trend = "BULLISH";
        } else {
            trend = "BEARISH";
        }


        // -------------------------------------------------
        // BUY
        // -------------------------------------------------


        if (buySignal) {


            Candle latestCandle =
                    candles.get(candles.size() - 1);


            Double price =
                    latestCandle.getClose();


            log.info(
                    "BUY signal: symbol={}, price={}, fastEMA={}, slowEMA={}",
                    symbol,
                    price,
                    currentFastEMA,
                    currentSlowEMA
            );


            return TradingSignal.builder()
                    .symbol(symbol)
                    .action("BUY")
                    .price(price)
                    .quantity(ORDER_QUANTITY)
                    .fastEma(currentFastEMA)
                    .slowEma(currentSlowEMA)
                    .trend(trend)
                    .timestamp(latestCandle.getTimestamp())
                    .build();
        }


        // -------------------------------------------------
        // SELL
        // -------------------------------------------------


        if (sellSignal) {


            Candle latestCandle =
                    candles.get(candles.size() - 1);


            Double price =
                    latestCandle.getClose();


            log.info(
                    "SELL signal: symbol={}, price={}, fastEMA={}, slowEMA={}",
                    symbol,
                    price,
                    currentFastEMA,
                    currentSlowEMA
            );


            return TradingSignal.builder()
                    .symbol(symbol)
                    .action("SELL")
                    .price(price)
                    .quantity(ORDER_QUANTITY)
                    .fastEma(currentFastEMA)
                    .slowEma(currentSlowEMA)
                    .trend(trend)
                    .timestamp(latestCandle.getTimestamp())
                    .build();
        }


        // No crossover
        return null;
    }


    /**
     * Calculate EMA using the standard EMA formula:
     *
     * EMA = (Close - Previous EMA) * Multiplier + Previous EMA
     *
     * Multiplier = 2 / (period + 1)
     */
    private Double calculateEMA(
            List<Double> prices,
            int period) {


        if (prices == null || prices.size() < period) {
            return null;
        }


        // TradingView-compatible initial EMA seed:
        // SMA of first 'period' values
        Double sum = 0.0;


        for (int i = 0; i < period; i++) {
            sum = sum + (prices.get(i));
        }


        BigDecimal ema =
                BigDecimal.valueOf(sum).divide(
                        BigDecimal.valueOf(period),
                        10,
                        RoundingMode.HALF_UP
                );


        BigDecimal multiplier =
                BigDecimal.valueOf(2)
                        .divide(
                                BigDecimal.valueOf(period + 1),
                                10,
                                RoundingMode.HALF_UP
                        );


        for (int i = period; i < prices.size(); i++) {


            Double close = prices.get(i);


            ema = BigDecimal.valueOf(close)
                    .subtract(ema)
                    .multiply(multiplier)
                    .add(ema);
        }


        return ema.doubleValue();
    }
}
