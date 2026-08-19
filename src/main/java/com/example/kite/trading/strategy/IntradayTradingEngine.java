package com.example.kite.trading.strategy;


import com.example.kite.trading.config.StrategyProperties;
import com.example.kite.trading.dto.Candle;
import com.example.kite.trading.dto.IndicatorSnapshot;
import com.example.kite.trading.dto.TradingSignal;
import com.example.kite.trading.indicator.ATRIndicator;
import com.example.kite.trading.indicator.EMAIndicator;
import com.example.kite.trading.indicator.RSIIndicator;
import com.example.kite.trading.indicator.VWAPIndicator;
import com.example.kite.trading.indicator.VolumeSMAIndicator;
import com.example.kite.trading.util.CandleValidator;
import com.example.kite.util.AlertSoundListener;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;


@Component
@RequiredArgsConstructor
public class IntradayTradingEngine {
    private static final Logger log = LoggerFactory.getLogger(IntradayTradingEngine.class);
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Kolkata");


    private final StrategyProperties properties;
    private final StrategyEvaluator strategyEvaluator;


    private EMAIndicator ema20;
    private EMAIndicator ema50;
    private ATRIndicator atr14;
    private RSIIndicator rsi14;
    private VWAPIndicator vwap;
    private VolumeSMAIndicator volumeSma20;


    private Candle previousCandle;
    private boolean initialized;


    private void initializeIndicators() {
        ema20 = new EMAIndicator(properties.getEmaFastPeriod());
        ema50 = new EMAIndicator(properties.getEmaSlowPeriod());
        atr14 = new ATRIndicator(properties.getAtrPeriod());
        rsi14 = new RSIIndicator(properties.getRsiPeriod());
        vwap = new VWAPIndicator();
        volumeSma20 = new VolumeSMAIndicator(properties.getVolumeSmaPeriod());
        initialized = true;
    }


    /**
     * Processes one completed candle. The method is intentionally stateful:
     * after warm-up each indicator performs constant-time work.
     */
    public TradingSignal onCandle(Candle candle) {
        try {
            CandleValidator.validate(candle);
        }catch(IllegalArgumentException e) {
            log.error("Invalid candle", e);
            return null;
        }


        if (!initialized) initializeIndicators();


        double ema20Value = ema20.update(candle.getClose());
        double ema50Value = ema50.update(candle.getClose());
        double atrValue = atr14.update(candle.getHigh(), candle.getLow(), candle.getClose());
        double rsiValue = rsi14.update(candle.getClose());


        LocalDate sessionDate = candle.getTimestamp().atZone(MARKET_ZONE).toLocalDate();
        double vwapValue = vwap.update(
                sessionDate,
                candle.getHigh(),
                candle.getLow(),
                candle.getClose(),
                candle.getVolume(),
                properties.isResetVwapOnSessionChange()
        );


        double volumeSmaValue = volumeSma20.update(candle.getVolume());


        boolean ready = ema20.isReady()
                && ema50.isReady()
                && atr14.isReady()
                && rsi14.isReady()
                && volumeSma20.isReady()
                && previousCandle != null;


        if (!ready) {
            previousCandle = candle;
            log.debug("Engine warming up symbol={} timestamp={}", candle.getSymbol(), candle.getTimestamp());
            return TradingSignal.builder().action("NONE").indicators( new IndicatorSnapshot(
                    safeValue(ema20), safeValue(ema50), safeValue(atr14),
                    safeValue(rsi14), vwapValue, volumeSmaValue,
                    previousCandle.getHigh(), previousCandle.getLow())).trend("Indicators are warming up").build();
        }


        IndicatorSnapshot snapshot = new IndicatorSnapshot(
                ema20Value,
                ema50Value,
                atrValue,
                rsiValue,
                vwapValue,
                volumeSmaValue,
                previousCandle.getHigh(),
                previousCandle.getLow()
        );


        TradingSignal signal = strategyEvaluator.evaluate(candle, snapshot);
        previousCandle = candle;
        return signal;
    }


    /**
     * Replays historical candles through the exact same incremental path.
     * This is O(n) in the number of candles.
     */
    public void initializeFromHistory(Iterable<Candle> candles) {
        reset();
        for (Candle candle : candles) {
            onCandle(candle);
        }
    }


    public void reset() {
        initialized = false;
        previousCandle = null;
        ema20 = null;
        ema50 = null;
        atr14 = null;
        rsi14 = null;
        vwap = null;
        volumeSma20 = null;
    }


    private static double safeValue(EMAIndicator indicator) {
        return indicator.isReady() ? indicator.getValue() : Double.NaN;
    }


    private static double safeValue(ATRIndicator indicator) {
        return indicator.isReady() ? indicator.getValue() : Double.NaN;
    }


    private static double safeValue(RSIIndicator indicator) {
        return indicator.isReady() ? indicator.getValue() : Double.NaN;
    }
}
