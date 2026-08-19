package com.example.kite.service;


import com.example.kite.dto.TickMessage;
import com.zerodhatech.models.HistoricalData;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
@Slf4j
public class OfflineTradingService {


    private MarketDataService marketDataService;
    private KiteTickerService kiteTickerService;


    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");


    public void performOfflineTrading(String exchange, String symbol) {


        List<TickMessage> stockData =  marketDataService.getAllHistoricalData(exchange, symbol);
        int batchSize = 60;
        log.info("Total records found size={}", stockData.size());
        for (int start = 0; start < stockData.size(); start += batchSize) {
            int end = Math.min(start + batchSize, stockData.size());


            List<TickMessage> batch = stockData.subList(start, end);
            log.info("Batch no={}, Batch size={}", start, batch.size() );
            List<HistoricalData> batchHistory = batch.stream().map(message-> {
                log.info("""
                   message.open={},
                   message.close={},
                   messahe.high={},
                   message.low={}
                   for tickMessageId={}
                   """, message.openPrice(), message.closePrice(), message.highPrice(), message.lastPrice(),message.id());
                HistoricalData hist = new HistoricalData();
                hist.open = message.openPrice();
                hist.close = message.closePrice();
                hist.high = message.highPrice();
                hist.low = message.lowPrice();
                hist.volume = message.volume();
                if(message.timestamp() != null) {
                    hist.timeStamp = message.timestamp().format(formatter);
                }
                return hist;
            }).collect(Collectors.toList());
            kiteTickerService.processBatch(batch.getLast(),batchHistory);
            log.info("Batch no={} completed successfully", start );
        }
    }


    private void processSingleCandle(List<TickMessage> stockData) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        for(TickMessage message: stockData) {
            log.info("""
                   message.open={},
                   message.close={},
                   messahe.high={},
                   message.low={}
                   for tickMessageId={}
                   """, message.openPrice(), message.closePrice(), message.highPrice(), message.lastPrice(),message.id());
            HistoricalData hist = new HistoricalData();
            hist.open = message.openPrice();
            hist.close = message.closePrice();
            hist.high = message.highPrice();
            hist.low = message.lowPrice();
            hist.volume = message.volume();
            if(message.timestamp() != null) {
                hist.timeStamp = message.timestamp().format(formatter);
            }
            kiteTickerService.generateSignalsByTickData(message, List.of(hist));
           /*try {
               Thread.sleep(2000);
           } catch (InterruptedException e) {
               throw new RuntimeException(e);
           }*/
        }
    }
}
