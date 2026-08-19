package com.example.kite.trading.strategy;


import com.example.kite.trading.config.StrategyProperties;
import com.example.kite.trading.dto.Candle;
import com.example.kite.trading.dto.IndicatorSnapshot;
import com.example.kite.trading.dto.TradingSignal;
import com.example.kite.trading.enums.SignalType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class StrategyEvaluator {
    private static final Logger log = LoggerFactory.getLogger(StrategyEvaluator.class);


    private final StrategyProperties properties;


    public TradingSignal evaluate(Candle candle, IndicatorSnapshot snapshot) {
        double price = candle.getClose();
        log.info("""
               snapshot.ema20={},
               snapshot.ema50={},
               price={},
               snapshot.vwap={},
               snapshot.previousHigh={},
               snapshot.previousLow={},
               snapshot.rsi14={},
               candle.volume={},
               snapshot.volumeSma20={},
               snapshot.atr14={},
              
               """, snapshot.ema20(), snapshot.ema50(),price, snapshot.vwap(), snapshot.previousHigh(), snapshot.previousLow(),
                snapshot.rsi14(), candle.getVolume(),snapshot.volumeSma20(),snapshot.atr14());
        boolean buy = snapshot.ema20() > snapshot.ema50()
                && price > snapshot.ema20()
                && price > snapshot.vwap()
                && price > snapshot.previousHigh()
                && snapshot.rsi14() > properties.getBuyRsiThreshold()
                && candle.getVolume() > snapshot.volumeSma20()
                && snapshot.atr14() > properties.getMinimumAtr();


        boolean sell = snapshot.ema20() < snapshot.ema50()
                && price < snapshot.ema20()
                && price < snapshot.vwap()
                && price < snapshot.previousLow()
                && snapshot.rsi14() < properties.getSellRsiThreshold()
                && candle.getVolume() > snapshot.volumeSma20()
                && snapshot.atr14() > properties.getMinimumAtr();


        if (buy) {
            double risk = snapshot.atr14() * properties.getStopLossAtrMultiplier();
            double stopLoss = price - risk;
            double target = price + (risk * properties.getRiskRewardRatio());
            log.info("BUY signal symbol={} price={} ema20={} ema50={} rsi={} atr={} vwap={}, risk={}, stoplosss={}, target={}",
                    candle.getSymbol(), price, snapshot.ema20(), snapshot.ema50(),
                    snapshot.rsi14(), snapshot.atr14(), snapshot.vwap(), risk, stopLoss, target);
            return TradingSignal.builder()
                    .symbol(candle.getSymbol())
                    .timestamp(candle.getTimestamp())
                    .price(price)
                    .action(SignalType.BUY.name())
                    .stopLoss(stopLoss)
                    .entryPrice(price)
                    .trend("All BUY conditions satisfied")
                    .target(target)
                    .indicators(snapshot)
                    .build();
        }


        if (sell) {
            double risk = snapshot.atr14() * properties.getStopLossAtrMultiplier();
            double stopLoss = price + risk;
            double target = price - (risk * properties.getRiskRewardRatio());
            log.info("SELL signal symbol={} price={} ema20={} ema50={} rsi={} atr={} vwap={}, risk={}, stoplosss={}, target={}",
                    candle.getSymbol(), price, snapshot.ema20(), snapshot.ema50(),
                    snapshot.rsi14(), snapshot.atr14(), snapshot.vwap(), risk, stopLoss, target);
            return TradingSignal.builder()
                    .symbol(candle.getSymbol())
                    .timestamp(candle.getTimestamp())
                    .price(price)
                    .action(SignalType.SELL.name())
                    .stopLoss(stopLoss)
                    .entryPrice(price)
                    .trend("All SELL conditions satisfied")
                    .target(target)
                    .indicators(snapshot)
                    .build();


        }


        log.info("No trading signal symbol={} price={}", candle.getSymbol(), price);
        return TradingSignal.builder().action("NONE").trend("No complete BUY/SELL setup").build();
    }
}
