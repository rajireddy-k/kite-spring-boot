package com.example.kite.signal.engine;


import com.example.kite.dto.Candle;
import com.example.kite.dto.TradingSignal;
import com.example.kite.enums.SignalType;
import com.example.kite.service.ConfigManagerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Component
@Slf4j
@AllArgsConstructor
public class IntradayTradingEngineV2 {


    /*
     * Configuration
     */
    private static final int LOOKBACK_CANDLES = 10;
    private static final String LOOKBACK_CANDLES_KEY = "LOOKBACK_CANDLES";


    private static final int FAST_EMA = 20;
    private static final int SLOW_EMA = 50;
    private static final int AVG_VOLUME_PERIOD = 20;
    private static final double MIN_ATR = 5.0;
    private static final String MIN_ATR_KEY = "MIN_ATR";




   /*
   EMA20 < EMA50
   Current Price < EMA20
   Current Price < Previous Candle Low
   Current Volume > Average Volume
   ATR > Minimum ATR */


    private final ConfigManagerService configManagerService;
    /*
     * Main calculation
     */
    public TradingSignal calculateSignal(
            List<Candle> historicalCandles,
            double currentPrice) {
        Integer lookbackCandleSize = (Integer) configManagerService.getOrDefaultValue(LOOKBACK_CANDLES_KEY, LOOKBACK_CANDLES);
        if (historicalCandles == null ||
                historicalCandles.size() < lookbackCandleSize) {
            log.info("Not enough historical candles to calculate signal. Required: {}, Provided: {}",
                    lookbackCandleSize, historicalCandles == null ? 0 : historicalCandles.size());
            return null;
        }


        historicalCandles = historicalCandles.stream()
                .sorted(Comparator.comparingLong(Candle::getTimestamp))
                .toList();


        /*
         * ------------------------------------------------
         * 3. Calculate ATR for top 14 candles data
         * ------------------------------------------------
         */
        double atr14 = calculateATR(historicalCandles,14);


        /*
         * Use only the latest N candles
         */
        List<Candle> candles =
                historicalCandles.subList(
                        historicalCandles.size()-lookbackCandleSize,
                        historicalCandles.size());






        /*
         * ------------------------------------------------
         * 4. Calculate momentum
         *
         * Positive = bullish
         * Negative = bearish
         * ------------------------------------------------
         */


        LocalDateTime timestamp = LocalDateTime.now();


        /* -----------------------------------------------------------------------------------*/
        double ema20 = calculateEMA(candles,FAST_EMA);


        double ema50 = calculateEMA(candles,SLOW_EMA);


        double averageVolume = calculateAverageVolume(candles);


        Candle previous = candles.get(candles.size()-2);


        double previousHigh = previous.getHigh();


        double previousLow = previous.getLow();


        double currentVolume = candles.get(candles.size()-1).getVolume();


        boolean bullishTrend =
                ema20 > ema50;


        boolean aboveEMA =
                currentPrice > ema20;


        boolean breakout =
                currentPrice > previousHigh;


        boolean volumeConfirmation =
                currentVolume > averageVolume;


        boolean volatilityGood =
                atr14 >= ((Double)configManagerService.getOrDefaultValue(MIN_ATR_KEY, MIN_ATR));


        if (bullishTrend &&
                aboveEMA &&
                breakout &&
                volumeConfirmation &&
                volatilityGood) {


            double entry = currentPrice;


            double stopLoss =
                    Math.min(ema20,
                            previousLow) - atr14;


            double risk =
                    entry - stopLoss;


            double target =
                    entry + (risk * 2);


            return new TradingSignal(
                    SignalType.BUY,
                    entry,
                    target,
                    stopLoss,
                    risk,
                    2,
                    100,
                    atr14,
                    ema20,
                    previousHigh,
                    previousLow,
                    "EMA20 > EMA50 Trend Buy",
                    90,
                    timestamp
            );
        }


        boolean bearishTrend =
                ema20 < ema50;


        boolean belowEMA =
                currentPrice < ema20;


        boolean breakdown =
                currentPrice < previousLow;


        if (bearishTrend &&
                belowEMA &&
                breakdown &&
                volumeConfirmation &&
                volatilityGood) {


            double entry = currentPrice;


            double stopLoss =
                    Math.max(ema20,
                            previousHigh) + atr14;


            double risk =
                    stopLoss - entry;


            double target =
                    entry - (risk * 2);


            return new TradingSignal(
                    SignalType.SELL,
                    entry,
                    target,
                    stopLoss,
                    risk,
                    2,
                    -100,
                    atr14,
                    ema20,
                    previousHigh,
                    previousLow,
                    "EMA20 < EMA50 Trend Sell",
                    90,
                    timestamp
            );
        }


        log.info("""
               EMA20             : {}
               EMA50             : {}
               ATR               : {}
               Current Price     : {}
               Previous High     : {}
               Previous Low      : {}
               Current Volume    : {}
               Average Volume    : {}
               Bullish Trend     : {}
               Bearish Trend     : {}
               Breakout          : {}
               Breakdown         : {}
               Volume Confirm    : {}
               """,
                ema20,
                ema50,
                atr14,
                currentPrice,
                previousHigh,
                previousLow,
                currentVolume,
                averageVolume,
                bullishTrend,
                bearishTrend,
                breakout,
                breakdown,
                volumeConfirmation);
        /*
         * ------------------------------------------------
         * 8. No signal
         * ------------------------------------------------
         */


        return null;
    }




    /*
     * ----------------------------------------------------
     * ATR CALCULATION
     * ----------------------------------------------------
     */
    private double calculateATR(List<Candle> candles, int period) {


        if (candles.size() <= period)
            return 0;


        List<Double> trList = new ArrayList<>();


        for (int i = 1; i < candles.size(); i++) {


            Candle current = candles.get(i);
            Candle previous = candles.get(i - 1);


            double tr = Math.max(
                    current.getHigh() - current.getLow(),
                    Math.max(
                            Math.abs(current.getHigh() - previous.getClose()),
                            Math.abs(current.getLow() - previous.getClose())
                    )
            );


            trList.add(tr);
        }


        double atr = trList.subList(0, period)
                .stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);


        for (int i = period; i < trList.size(); i++) {


            atr = ((atr * (period - 1)) + trList.get(i)) / period;
        }


        return atr;
    }


    private double calculateAverageVolume(List<Candle> candles) {


        return candles.stream()
                .mapToDouble(Candle::getVolume)
                .average()
                .orElse(0);
    }


    private double calculateEMA(List<Candle> candles, int period) {


        if (candles.size() < period)
            return candles.getLast().getClose();


        double multiplier = 2.0 / (period + 1);


        double ema = candles
                .subList(0, period)
                .stream()
                .mapToDouble(Candle::getClose)
                .average()
                .orElse(0);


        for (int i = period; i < candles.size(); i++) {


            ema = ((candles.get(i).getClose() - ema) * multiplier) + ema;
        }


        return ema;
    }


}
