package com.example.kite.scheduler;

import com.example.kite.dto.TickMessage;
import com.example.kite.service.ConfigManagerService;
import com.example.kite.service.MarketDataService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@AllArgsConstructor
public class TrendStockFinderService {

    private final MarketDataService marketDataService;
    private final ConfigManagerService configManagerService;

    @Scheduled(cron = "${trading.cron.job.schedule-time}")
    public void findTodayBullishAndBearishStocks() {
        log.info("scheduler running at : {]", LocalDateTime.now());
        List<TickMessage> stockTicks = marketDataService.findListOfPreferredSymols();
        if(!stockTicks.isEmpty()){
            log.info("Found Top picked stocks for today trading :", stockTicks.size());
            Map<Boolean, List<TickMessage>> partitioned =
                    stockTicks.stream()
                            .collect(Collectors.partitioningBy(
                                    tick -> tick.openPrice() == tick.lowPrice()
                            ));

            List<TickMessage> openEqualsLow = partitioned.get(true);
            if(openEqualsLow != null) {
                Set<String> buySymbols = openEqualsLow.stream().map(TickMessage::symbol).collect(Collectors.toSet());
               configManagerService.addWatchList("BUY", buySymbols);
            }
            List<TickMessage> openNotEqualsLow = partitioned.get(false);
            if(openNotEqualsLow != null) {
                Set<String> sellSymbols = openNotEqualsLow.stream().map(TickMessage::symbol).collect(Collectors.toSet());
                configManagerService.addWatchList("SELL", sellSymbols);
            }
        }
    }

}
