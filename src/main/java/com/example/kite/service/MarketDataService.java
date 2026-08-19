package com.example.kite.service;


import com.example.kite.dto.OrderData;
import com.example.kite.dto.TickMessage;
import com.example.kite.dto.TradingSignal;
import com.example.kite.dto.TradingViewSignal;
import com.example.kite.entity.OrderDataEntity;
import com.example.kite.entity.StockTickEntity;
import com.example.kite.entity.TradingSignalEntity;
import com.example.kite.mapper.KiteTickMessageMapper;
import com.example.kite.repository.KiteOrderDataRepository;
import com.example.kite.repository.KiteSignalRepository;
import com.example.kite.repository.StockTickRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
public class MarketDataService {


    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final StockTickRepository tickRepository;
    private final Map<Long, String> tokenToSymbol = new ConcurrentHashMap<>();
    private final KiteTickMessageMapper tickMessageMapper;
    private final KiteSignalRepository signalRepository;
    private final KiteOrderDataRepository orderDataRepository;


    public MarketDataService(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            StockTickRepository tickRepository,
            KiteTickMessageMapper tickMessageMapper,
            KiteSignalRepository signalRepository,
            KiteOrderDataRepository orderDataRepository) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.tickRepository = tickRepository;
        this.tickMessageMapper = tickMessageMapper;
        this.signalRepository = signalRepository;
        this.orderDataRepository = orderDataRepository;
    }


    public void register(long token, String exchange, String symbol) {
        log.info("Registering token {} for {}:{}", token, exchange, symbol);
        tokenToSymbol.put(token, exchange.toUpperCase() + ":" + symbol.toUpperCase());
    }


    @Transactional
    public TickMessage process(Tick tick) {
        String key = tokenToSymbol.get(tick.getInstrumentToken());
        if (key == null) {
            log.debug("Received tick for unregistered token: {} hence returning null", tick.getInstrumentToken());
            return null;
        }


        String[] parts = key.split(":", 2);
        String exchange = parts[0];
        String symbol = parts[1];


        LocalDateTime timestamp = LocalDateTime.now();
        if (tick.getTickTimestamp() != null) {
            timestamp = tick.getTickTimestamp().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }
        log.debug("Processing tick for {}:{} at {}", exchange, symbol, timestamp);
        TickMessage message = tickMessageMapper.mapToTickMessageFromTicker(exchange, symbol, tick, timestamp);


        try {
            String json = objectMapper.writeValueAsString(message);
            log.info("Tick Message --> {}", json);
            redis.opsForValue().set("stock:latest:" + key, json);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to write latest tick to Redis", e);
        }
        StockTickEntity entity = tickMessageMapper.mapToStockTickEntityFromTicker(exchange, symbol, tick, timestamp);


        tickRepository.save(entity);
        return message;
    }


    @Transactional
    public void saveTradingSignal(TradingSignal signal, String source) {
        String symbol = tokenToSymbol.get(signal.getInstrumentId());
        TradingSignalEntity entity = tickMessageMapper.mapToTradingSignalEntity(symbol, signal, source);
        signalRepository.save(entity);
    }


    @Transactional
    public void saveWebHookTradingSignal(TradingViewSignal signal, String source) {

        TradingSignalEntity entity = tickMessageMapper.mapToTradingSignalEntity(signal, source);
        entity.setTimestamp(Timestamp.valueOf(LocalDateTime.now()));
        signalRepository.save(entity);
    }


    public String latest(String exchange, String symbol) {
        log.debug("Fetching latest tick from Redis for {}:{}", exchange, symbol);
        String key = "stock:latest:" + exchange.toUpperCase() + ":" + symbol.toUpperCase();
        String latestStockPrice =  redis.opsForValue().get(key);
        if (latestStockPrice == null) {
            latestStockPrice = latestFromDb(exchange, symbol);
        }
        return latestStockPrice;
    }


    public String latestFromDb(String exchange, String symbol) {
        log.debug("Fetching latest tick from DB for {}:{}", exchange, symbol);
        return tickRepository.findTopByExchangeAndSymbolOrderByTickTimeDesc(
                        exchange.toUpperCase(), symbol.toUpperCase())
                .map(entity -> {
                    try {
                        TickMessage message = tickMessageMapper.mapToTickMessageFromEntity(entity);
                        return objectMapper.writeValueAsString(message);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .orElse(null);
    }


    public List<TickMessage> getAllHistoricalData(String exchange, String symbol) {
        List<StockTickEntity> stockhistory = tickRepository.findAllStocksForASymbol(exchange.toUpperCase(), symbol.toUpperCase());
        return stockhistory.stream().map(tickMessageMapper::mapToTickMessageFromEntity).toList();
    }

    @Transactional
    public void saveOrderData(OrderData orderData) {
        OrderDataEntity entity = tickMessageMapper.mapToOrderDataEntity(orderData);
        orderDataRepository.save(entity);
    }

    public List<TickMessage> findListOfPreferredSymols() {
        LocalDateTime startOfDay = LocalDate.now().atTime(9, 15);
        LocalDateTime endOfDay = LocalDate.now().atTime(9, 30);

        List<StockTickEntity> topBullishOrBearishStocks = tickRepository.findOpeningRangeTicks(
                java.sql.Timestamp.valueOf(startOfDay),java.sql.Timestamp.valueOf(endOfDay));
        if(CollectionUtils.isEmpty(topBullishOrBearishStocks)) {
            return Collections.emptyList();
        }
        return topBullishOrBearishStocks.stream().map(
                entity-> {
                    return tickMessageMapper.mapToTickMessageFromEntity(entity);
                }
        ).toList();
    }
}
