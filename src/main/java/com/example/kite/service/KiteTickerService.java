package com.example.kite.service;


import com.example.kite.config.KiteProperties;
import com.example.kite.dto.Candle;
import com.example.kite.dto.TickMessage;
import com.example.kite.dto.TradingSignal;
import com.example.kite.dto.UserData;
import com.example.kite.enums.SignalSourceType;
import com.example.kite.enums.SignalType;
import com.example.kite.signal.engine.IntradayTradingEngineV2;
import com.example.kite.trading.strategy.IntradayTradingEngine;
import com.example.kite.trading.strategy.ema5by20.EmaCrossoverStrategy;
import com.example.kite.util.AlertSoundListener;
import com.example.kite.util.DateUtils;
import com.example.kite.websocket.MarketWebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.HistoricalData;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnConnect;
import com.zerodhatech.ticker.OnDisconnect;
import com.zerodhatech.ticker.OnError;
import com.zerodhatech.ticker.OnTicks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.awt.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


@Service
@Slf4j
public class KiteTickerService {


    private static final String ENABLE_MULTI_CANDLE_SIGNAL_KEY = "ENABLE_MULTI_CANDLE_SIGNAL";
    private static final boolean ENABLE_MULTI_CANDLE_SIGNAL = false;


    private static final String ENABLE_SINGLE_CANDLE_SIGNAL_KEY = "ENABLE_SINGLE_CANDLE_SIGNAL";
    private static final boolean ENABLE_SINGLE_CANDLE_SIGNAL = false;


    private static final String ENABLE_EMA_CROSSOVER_IND_KEY = "ENABLE_EMA_CROSSOVER_IND";
    private static final boolean ENABLE_EMA_CROSSOVER_IND = false;




    private final KiteConnect kite;
    private final KiteProperties properties;
    private final MarketDataService marketDataService;
    private final MarketWebSocketHandler webSocketHandler;
    private final ZerodhaService zerodhaService;
    private final IntradayTradingEngineV2 tradingEngine;
    private final ObjectMapper objectMapper;
    private final IntradayTradingEngine intradayTradingEngine;
    private final ConfigManagerService configManagerService;
    private final EmaCrossoverStrategy emaCrossoverStrategy;
    private final StringRedisTemplate redis;


    private final Set<Long> subscribedTokens = ConcurrentHashMap.newKeySet();
    private volatile KiteTicker ticker;
    private volatile boolean connected;


    public KiteTickerService(
            KiteConnect kite,
            KiteProperties properties,
            MarketDataService marketDataService,
            MarketWebSocketHandler webSocketHandler,
            ZerodhaService zerodhaService,
            IntradayTradingEngineV2 tradingEngine,
            ObjectMapper objectMapper,
            IntradayTradingEngine intradayTradingEngine,
            ConfigManagerService configManagerService,
            EmaCrossoverStrategy emaCrossoverStrategy,
            StringRedisTemplate redis) {
        this.kite = kite;
        this.properties = properties;
        this.marketDataService = marketDataService;
        this.webSocketHandler = webSocketHandler;
        this.zerodhaService = zerodhaService;
        this.tradingEngine = tradingEngine;
        this.objectMapper = objectMapper;
        this.intradayTradingEngine = intradayTradingEngine;
        this.configManagerService = configManagerService;
        this.emaCrossoverStrategy = emaCrossoverStrategy;
        this.redis = redis;
        //refreshSession();
    }

    private void refreshSession() {
        log.info("assigning prev active token....");

        String userSession = redis.opsForValue().get(kite.getApiKey());
        if (userSession != null) {
            try {
                UserData session = objectMapper.readValue(userSession, UserData.class);
                if (session != null) {
                    kite.setAccessToken(session.accessToken());
                    kite.setPublicToken(session.publicToken());
                    kite.setUserId(session.userId());
                    log.info("Token assigned successfully");
                    start(session.accessToken());
                }
            } catch (JsonProcessingException e) {
                log.error("invalid format of session stored in cache");
            }
        }
    }


    /**
     * Starts the KiteTickerService with the provided access token.
     * @param accessToken
     */
    public synchronized void start(String accessToken) {
        log.info("Starting KiteTickerService with access token: {}", accessToken);
        if (connected && ticker != null) {
            return;
        }
        ticker = new KiteTicker(accessToken, kite.getApiKey());

        ticker.setOnConnectedListener(new OnConnect() {
            @Override
            public void onConnected() {
                connected = true;
                resubscribe();
            }
        });

        ticker.setOnDisconnectedListener(new OnDisconnect() {
            @Override
            public void onDisconnected() {
                connected = false;
            }
        });


        ticker.setOnErrorListener(new OnError() {
            @Override
            public void onError(Exception e) {


            }

            @Override
            public void onError(KiteException e) {
                connected = false;
            }


            @Override
            public void onError(String s) {


            }
        });


        ticker.setOnTickerArrivalListener(new OnTicks() {
            @Override
            public void onTicks(ArrayList<Tick> ticks) {
                for (Tick tick : ticks) {
                    try {
                        log.info(
                                "TICK instrumentToken={} timestamp={} lastPrice={} open={} high={} low={} close={} volume={}",
                                tick.getInstrumentToken(),
                                tick.getTickTimestamp(),
                                tick.getLastTradedPrice(),
                                tick.getOpenPrice(),
                                tick.getHighPrice(),
                                tick.getLowPrice(),
                                tick.getClosePrice(),
                                tick.getVolumeTradedToday()
                        );
                        var message = marketDataService.process(tick);
                        if (message != null) {
                            processCurrentTickMessage(message);
                        }


                    } catch (Exception ignored) {
                        // Replace with structured logging/metrics in production.
                    }
                }
            }
        });


        ticker.setTryReconnection(true);
        ticker.connect();
    }


    public synchronized void subscribe(long token) {
        log.info("Subscribing to token: {}", token);
        subscribedTokens.add(token);
        if (ticker != null && connected) {
            ArrayList<Long> tokens = new ArrayList<>();
            tokens.add(token);
            ticker.subscribe(tokens);
            ticker.setMode(tokens, mode());
        }
    }


    public synchronized void unsubscribe(long token) {
        log.info("Unsubscribing from token: {}", token);
        subscribedTokens.remove(token);
        if (ticker != null && connected) {
            ArrayList<Long> tokens = new ArrayList<>();
            tokens.add(token);
            ticker.unsubscribe(tokens);
        }
    }

    private void resubscribe() {
        log.info("Resubscribing to tokens: {}", subscribedTokens);
        if (ticker == null || subscribedTokens.isEmpty()) {
            return;
        }
        ArrayList<Long> tokens = new ArrayList<>(subscribedTokens);
        ticker.subscribe(tokens);
        ticker.setMode(tokens, mode());
    }

    private String mode() {
        return switch (properties.getWebsocketMode().toLowerCase()) {
            case "ltp" -> KiteTicker.modeLTP;
            case "full" -> KiteTicker.modeFull;
            default -> KiteTicker.modeQuote;
        };
    }

    public boolean isConnected() {
        return connected;
    }

    public synchronized void stop() {
        if (ticker != null) {
            ticker.disconnect();
        }
        connected = false;
        ticker = null;
        subscribedTokens.clear();
    }


    public void processCurrentTickMessage(TickMessage message) throws JsonProcessingException {
        if (message != null) {


            List<HistoricalData> historyData = getHistoryData(message.instrumentToken());
            processBatch(message, historyData);


        }
    }


    public void processBatch(TickMessage message, List<HistoricalData> historyData) {
        boolean enableMultiCandleFlow = (Boolean) configManagerService.getOrDefaultValue(ENABLE_MULTI_CANDLE_SIGNAL_KEY, ENABLE_MULTI_CANDLE_SIGNAL);
        boolean enableSingleCandleFlow = (Boolean) configManagerService.getOrDefaultValue(ENABLE_SINGLE_CANDLE_SIGNAL_KEY, ENABLE_SINGLE_CANDLE_SIGNAL);
        boolean enableEMACrossOverIndicator = (Boolean) configManagerService.getOrDefaultValue(ENABLE_EMA_CROSSOVER_IND_KEY, ENABLE_EMA_CROSSOVER_IND);
        if(!enableMultiCandleFlow && !enableSingleCandleFlow && !enableEMACrossOverIndicator) {
            log.error("At least 1 flow should be enabled");
            return;
        }
        try (ExecutorService exService = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Boolean> result = null;
            if (enableMultiCandleFlow) {
                result = exService.submit(() -> generateSignalsByHistoricalData(message, historyData));
            }else{
                log.warn("Multi candle flow is disabled, skipping the Signal calculation by 10 candle logic");
            }
            Future<Boolean> result2 = null;
            if(enableSingleCandleFlow) {
                HistoricalData singleCandleSignal = historyData.getLast();
                result2 = exService.submit(() -> generateSignalsByTickData(message, List.of(singleCandleSignal)));
            }else{
                log.warn("Single candle flow is disabled, skipping the Signal calculation by Single candle logic");
            }
            Future<Boolean> result3 = null;
            if( enableEMACrossOverIndicator) {
                result3 = exService.submit(() -> generateSignalsByEMACrossOverStrategy(message, historyData));
            }


            if (result != null && result.isDone() ) {
                boolean signalTriggered = result.get();
                log.info("multi candle signals executed successfully, result from 10 candles={}", signalTriggered);
            }
            if (result2 != null && result2.isDone()) {
                boolean singlSsignalTriggered = result2.get();
                log.info("Single candle signal executed successfully, result from 1 candle={}", singlSsignalTriggered);
            }
            if (result3 != null && result3.isDone()) {
                boolean emaCrossOverSignal = result3.get();
                log.info("emaCrossOverSignal indicator executed successfully, result from All candle={}", emaCrossOverSignal);
            }


        } catch(Exception e) {
            log.error("Executer service Exception", e);
        }
    }


    public boolean generateSignalsByTickData(TickMessage message, List<HistoricalData> historyData) {

        try {
            if(!CollectionUtils.isEmpty(historyData)) {
                for(HistoricalData data: historyData) {
                    com.example.kite.trading.dto.Candle candle =
                            new com.example.kite.trading.dto.Candle(
                                    DateUtils.formatStringToLocalDate(data.timeStamp),
                                    data.open,
                                    data.high,
                                    data.low,
                                    data.close,
                                    data.volume,
                                    message.symbol());
                    log.info("""
                           SingleCandle Historical data-->
                           openPrice: {},
                           closePrice: {},
                           highPrice: {},
                           lowPrice: {},
                           volume: {},
                           timestamp: {}
                           """, data.open, data.close, data.high, data.low, data.volume, data.timeStamp);
                    com.example.kite.trading.dto.TradingSignal tradingSignal = intradayTradingEngine.onCandle(candle);


                    if (tradingSignal != null) {
                        TradingSignal tradeSignal = new TradingSignal();
                        if (com.example.kite.trading.enums.SignalType.NONE.name().equals(tradingSignal.getAction())) {
                            tradeSignal.setSignal(SignalType.NO_SIGNAL);
                        } else {
                            tradeSignal.setSignal(SignalType.valueOf(tradingSignal.getAction()));
                        }
                        tradeSignal.setEntry(tradingSignal.getEntryPrice() != null ? tradingSignal.getEntryPrice() : 0.0);
                        tradeSignal.setStopLoss(tradingSignal.getStopLoss()!=null ? tradingSignal.getStopLoss():0.0);
                        tradeSignal.setTarget(tradingSignal.getTarget() != null ? tradingSignal.getTarget(): 0.0);


                        tradeSignal.setTimestamp(tradingSignal.getTimestamp());
                        tradeSignal.setInstrumentId(message.instrumentToken());
                        if(tradeSignal.getSignal() != SignalType.NO_SIGNAL) {
                            marketDataService.saveTradingSignal(tradeSignal,"IntradayTradingEngine");
                            zerodhaService.createIntradayOrder(tradeSignal);
                            Thread.startVirtualThread(() -> triggerNotification(tradingSignal,SignalSourceType.IntradayTradingEngine));
                        }


                    }
                }
                return true;
            }
        }catch(Exception e) {
            log.error("Signal Error  -->"+e.getMessage());
            return false;
        }
        return false;
    }


    public boolean generateSignalsByEMACrossOverStrategy(TickMessage message, List<HistoricalData> historyData) {
        try {
            if(!CollectionUtils.isEmpty(historyData)) {
                List<com.example.kite.trading.dto.Candle> candleList = new ArrayList<>();
                for(HistoricalData data: historyData) {
                    com.example.kite.trading.dto.Candle candle =
                            new com.example.kite.trading.dto.Candle(
                                    DateUtils.formatStringToLocalDate(data.timeStamp),
                                    data.open,
                                    data.high,
                                    data.low,
                                    data.close,
                                    data.volume,
                                    message.symbol());
                    candleList.add(candle);
                    log.info("""
                           SingleCandle Historical data-->
                           openPrice: {},
                           closePrice: {},
                           highPrice: {},
                           lowPrice: {},
                           volume: {},
                           timestamp: {}
                           """, data.open, data.close, data.high, data.low, data.volume, data.timeStamp);
                }
                com.example.kite.trading.dto.TradingSignal tradingSignal = emaCrossoverStrategy.evaluate(message.symbol(), candleList);

                if (tradingSignal != null) {
                    TradingSignal tradeSignal = new TradingSignal();
                    if (com.example.kite.trading.enums.SignalType.NONE.name().equals(tradingSignal.getAction())) {
                        tradeSignal.setSignal(SignalType.NO_SIGNAL);
                    } else {
                        tradeSignal.setSignal(SignalType.valueOf(tradingSignal.getAction()));
                    }
                    tradeSignal.setEntry(tradingSignal.getEntryPrice());
                    tradeSignal.setStopLoss(tradingSignal.getStopLoss()!=null ? tradingSignal.getStopLoss():0.0);
                    tradeSignal.setTarget(tradingSignal.getTarget() != null ? tradingSignal.getTarget(): 0.0);


                    tradeSignal.setTimestamp(tradingSignal.getTimestamp());
                    tradeSignal.setInstrumentId(message.instrumentToken());
                    if(tradeSignal.getSignal() != SignalType.NO_SIGNAL) {
                        marketDataService.saveTradingSignal(tradeSignal,"emaCrossoverStrategy");
                        zerodhaService.createIntradayOrder(tradeSignal);
                        Thread.startVirtualThread(() -> triggerNotification(tradingSignal,SignalSourceType.emaCrossoverStrategy));
                    }
                }
                return true;
            }
        }catch(Exception e) {
            log.error("Signal Error  -->"+e.getMessage());
            return false;
        }
        return false;
    }

    private void triggerNotification(Object tradingSignal, SignalSourceType source) {
        AlertSoundListener.beep(source);
        log.info("Broadcasting trading Signal message: {}", tradingSignal);
        webSocketHandler.broadcast(tradingSignal);
    }

    private boolean generateSignalsByHistoricalData(TickMessage message, List<HistoricalData> historicalDataArray) {
        boolean signalGenerated = false;
        try {
            List<Candle> candleList = new ArrayList<>();
            for (HistoricalData data : historicalDataArray) {
                log.info("History data{}", objectMapper.writeValueAsString(data));
                candleList.add(new Candle(DateUtils.DateToMilliseconds(data.timeStamp), data.open, data.high, data.low, data.close, data.volume));
            }
            TradingSignal tradeSignal = tradingEngine.calculateSignal(candleList, message.lastPrice());
            if (tradeSignal != null) {
                tradeSignal.setInstrumentId(message.instrumentToken());
                marketDataService.saveTradingSignal(tradeSignal, "IntradayTradingEngineV2");
                zerodhaService.createIntradayOrder(tradeSignal);
                Thread.startVirtualThread(() -> triggerNotification(tradeSignal, SignalSourceType.IntradayTradingEngineV2));
                signalGenerated = true;
            }
        }catch(Exception e) {
            log.info("Exception while generating the signals", e);
        }
        return signalGenerated;
    }


    private List<HistoricalData> getHistoryData(long instrumentId) throws JsonProcessingException {
        HistoricalData historicalData = zerodhaService.getHistoricalData(instrumentId);
        if (historicalData != null && historicalData.dataArrayList != null
                && !historicalData.dataArrayList.isEmpty()) {
            log.info(objectMapper.writeValueAsString(historicalData));
            return historicalData.dataArrayList;
        }
        return null;
    }
}
