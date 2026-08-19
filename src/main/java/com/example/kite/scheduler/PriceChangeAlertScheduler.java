package com.example.kite.scheduler;

import com.example.kite.alerts.IntradayTradingAlert;
import com.example.kite.dto.TradingViewSignal;
import com.example.kite.service.InstrumentService;
import com.example.kite.service.ZerodhaService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@AllArgsConstructor
public class PriceChangeAlertScheduler {

    private final ZerodhaService zerodhaService;
    private final IntradayTradingAlert intradayTradingAlert;

    private final Map<String, String> symbolToExchangeMap = new ConcurrentHashMap<>();

    public void subscribeSymbol(String symbol, String exchange) {
        symbolToExchangeMap.put(symbol, exchange);
    }
    //add schedled method to check price change alerts every 10 second
   // @Scheduled(fixedRate = 10000)
    public void checkPriceChangeAlerts() {
        if ( symbolToExchangeMap.isEmpty() ){
            log.info("No symbols to check for price change alerts");
            return;
        }
        symbolToExchangeMap.entrySet().forEach(entry -> {
            Double latestPrice = zerodhaService.getPriceForSymbol(entry.getValue(), entry.getKey());
            if (latestPrice != null) {
                log.info("Latest price for {}:{} is {}", entry.getValue(), entry.getKey(), latestPrice);
                IntradayTradingAlert.Alert alert = intradayTradingAlert.calculateAlert(latestPrice);
                if(alert != null ) {
                    log.info("Price change alert for {}:{} - {}", entry.getValue(), entry.getKey(), alert);
                    TradingViewSignal tradingViewSignal= new TradingViewSignal();
                    tradingViewSignal.setPrice(alert.getCurrentPrice());
                    tradingViewSignal.setSymbol(entry.getValue() + ":" + entry.getKey());
                    tradingViewSignal.setAction(alert.getSignal().name());
                    if(alert.getSignal() == IntradayTradingAlert.Signal.BUY) {
                        log.info("Placing BUY order for {}:{}", entry.getValue(), entry.getKey());
                        zerodhaService.placeOrder(tradingViewSignal);
                    } else if(alert.getSignal() == IntradayTradingAlert.Signal.SELL) {
                        log.info("Placing SELL order for {}:{}", entry.getValue(), entry.getKey());
                        zerodhaService.placeOrder(tradingViewSignal);
                    }else {
                        log.info("No action for {}:{}", entry.getValue(), entry.getKey());
                    }
                }
            } else {
                log.warn("Could not fetch latest price for {}:{}", entry.getValue(), entry.getKey());
            }
        });

    }
}
