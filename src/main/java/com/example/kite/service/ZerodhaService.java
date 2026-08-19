package com.example.kite.service;


import com.example.kite.dto.*;
import com.example.kite.exception.KiteClientException;
import com.example.kite.exception.ValidationException;
import com.example.kite.validate.ActionEnum;
import com.example.kite.validate.ExchangeEnum;
import com.example.kite.websocket.MarketWebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;


@Service
@Slf4j
public class ZerodhaService {

    private static final String TIME_INTERVAL_KEY = "TIME_INTERVAL";
    private static final String TIME_INTERVAL = "5minute";
    private static final String BACKOUT_TIME_PERIOD_KEY = "BACKOUT_TIME_PERIOD";
    private static final String BACKOUT_TIME_PERIOD = "60";
    private static final String PLACE_LIMIT_ORDER_KEY = "PLACE_LIMIT_ORDER";
    private static final String ENABLE_TO_PLACE_BUY_ORDER_KEY = "ENABLE_TO_PLACE_BUY_ORDER";
    private static final String ENABLE_TO_PLACE_SELL_ORDER_KEY = "ENABLE_TO_PLACE_SELL_ORDER";
    private static final String INTRADAY_MARGIN_THRESHOLD_KEY = "INTRADAY_MARGIN_THRESHOLD";
    private static final double INTRADAY_MARGIN_THRESHOLD = 0.75;
    private static final String PLACE_DELIVERY_ORDER_KEY = "PLACE_DELIVERY_ORDER";
    private static final String PLACE_INTRADAY_ORDER_KEY = "PLACE_INTRADAY_ORDER";

    private final KiteConnect kite;
    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper;
    private final InstrumentService instrumentService;
    private final KiteMarginCalculator kiteMarginCalculator;
    private  final ConfigManagerService configManagerService;
    private final StringRedisTemplate redis;
    private final MarketWebSocketHandler webSocketHandler;

    public ZerodhaService(KiteConnect kite,
                          MarketDataService marketDataService,
                          ObjectMapper objectMapper,
                          InstrumentService instrumentService,
                          KiteMarginCalculator kiteMarginCalculator,
                          ConfigManagerService configManagerService,
                          StringRedisTemplate redis,
                          MarketWebSocketHandler webSocketHandler){
        this.configManagerService = configManagerService;
        this.kiteMarginCalculator = kiteMarginCalculator;
        this.kite = kite;
        this.instrumentService = instrumentService;
        this.marketDataService = marketDataService;
        this.objectMapper = objectMapper;
        this.redis = redis;
        this.webSocketHandler = webSocketHandler;
    }

    private String generateOrderKey(String symbol) {
        String userId;
        try {
            userId = kite.getUserId();
        } catch (NullPointerException n) {
            userId = "Test";
        }
        return userId + "#" + symbol;
    }

    public String placeOrder(TradingViewSignal signal)  {
        Thread.startVirtualThread(()->marketDataService.saveWebHookTradingSignal(signal, signal.getSource() +"-WebHook"));
        String signalKey = generateOrderKey(signal.getSymbol());
        SignalCacheHolder cacheHolder = getOrderFromCache(signalKey, signal.getAction(), signal.getSymbol());
        boolean isReturnOrder = cacheHolder.isReturnOrder();
        boolean isDuplicateSignal = cacheHolder.isDuplicateSignal();
        if(isDuplicateSignal) {
            return "Duplicate event";
        }

        OrderParams params = cacheHolder.getParams();
        if(params == null || !isReturnOrder) {
            log.info("New Order request for symbol: {} and action:{}", signal.getSymbol(), signal.getAction());
            params = createOrderRequest(signal);
        } else {
            log.info("Exit Order request for symbol: {} and action:{}", signal.getSymbol(), signal.getAction());
        }
        OrderResponse order = placeZerodhaOrder(params, isReturnOrder, signalKey);
        signal.setQuantity(params.quantity);
        String orderId = saveOrderData(order, params);
        webSocketHandler.broadcast(signal);
        return orderId;
    }

    private OrderParams createOrderRequest(TradingViewSignal signal) {
        log.info("Creating intraday Order request with details: {}", signal);
        OrderParams params = new OrderParams();
        String [] values = signal.getSymbol().split(":");

        if(values.length == 2) {
            params.tradingsymbol = values[1];
            params.exchange = ExchangeEnum.valueOf(values[0]).name();
        }else {
            throw new ValidationException("Invalid symbol format. Expected format: EXCHANGE:SYMBOL");
        }

        params.transactionType = ActionEnum.valueOf(signal.getAction().toUpperCase()).name();
        Double price = signal.getPrice();
        if(price == null || price <= 0) {
            price = getPriceForSymbol(params.exchange, params.tradingsymbol);
        }
        if(price == null || price <= 0) {
            throw new ValidationException("Price not found for symbol: " + params.tradingsymbol);
        }
        boolean isIntraDayOrder = (Boolean)configManagerService.getOrDefaultValue(PLACE_INTRADAY_ORDER_KEY, false);
        boolean isDeliveryOrder = (Boolean)configManagerService.getOrDefaultValue(PLACE_DELIVERY_ORDER_KEY, true);
        double stockPrice = 0;
        if(isIntraDayOrder) {
            double marginPrice = (price / 5);
            try {
                MarginResult marginResult = kiteMarginCalculator.calculateMISMargin(params.exchange, params.tradingsymbol, params.transactionType, 1, price);
                marginPrice = marginResult.marginPerShare();
            } catch (KiteClientException kiteExp) {
                log.error("Kite client session expired, unable to connect kite service, using default margin price");
            }
            params.product = Constants.PRODUCT_MIS;
            log.info("Intraday margin price per share : {}, for symbol: {}", marginPrice, params.tradingsymbol);
            stockPrice = marginPrice;
        } else {
            stockPrice = price;
            params.product = Constants.PRODUCT_CNC;
        }

        int quantity = getMaxEligibleQuantity(params.tradingsymbol, stockPrice);


        if (quantity <= 0) {
            throw new ValidationException("In sufficient fund to make an order for  " + params.tradingsymbol);
        }
        params.quantity = quantity;

       Boolean limitOrder = (Boolean) configManagerService.getOrDefaultValue(PLACE_LIMIT_ORDER_KEY, Boolean.FALSE);
        if(limitOrder) {
            params.orderType = Constants.ORDER_TYPE_LIMIT;
            params.price = Math.ceil(signal.getPrice());
        } else {
            params.orderType = Constants.ORDER_TYPE_MARKET;
        }
        params.validity = Constants.VALIDITY_DAY;
        log.info("OrderRequest ---> {}", params);
        return params;
    }

    private void saveOrderToCache(String key, OrderParams value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redis.opsForValue().set(key, json, calculateDuration());
            log.info("Order request saved to cache for key:{}", key);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse the Json", e);
        } catch (Exception e) {
            log.error("Failed to save the orderRequest to cache", e);
        }
    }

    private Duration calculateDuration() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime target = LocalDateTime.of(
                LocalDate.now(),
                LocalTime.of(19, 15)
        );
        Duration duration = Duration.between(now, target);
        log.info("cache entry will expire after : {} hours",duration.toHours());
        return duration;
    }

    private SignalCacheHolder getOrderFromCache(String key, String action, String symbol) {
        SignalCacheHolder holder = new SignalCacheHolder();
        String json = null;
        try {
             json = redis.opsForValue().get(key);
             if(json != null) {
                 OrderParams params = objectMapper.readValue(json, OrderParams.class);
                 if (!action.equalsIgnoreCase(params.transactionType)) {
                     log.info("preparing returnOrder");
                     params.transactionType = ActionEnum.valueOf(action).name();
                     holder.setReturnOrder(true);
                     holder.setParams(params);
                 } else {
                     log.error("Duplicate Signal received for the Symbol={}", symbol);
                    holder.setDuplicateSignal(true);
                 }
             }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse the Json={}", json);
        }
        return holder;
    }


    public Double getPriceForSymbol(String exchange, String symbol) {
        Double price = null;
        String message = marketDataService.latest(exchange, symbol);
        if(message != null) {
            try {
                TickMessage tickMessage = objectMapper.readValue(message, TickMessage.class);
                price =  tickMessage.lastPrice();
            } catch (Exception e) {
                log.error("Error parsing tick message for {}:{}", exchange, symbol, e);
            }
        }
        if(price == null || price <= 0) {
            try {
                long instrumentId = instrumentService.tokenFor(exchange, symbol);
                Map<String, Quote> quotes = kite.getQuote(new String[]{String.valueOf(instrumentId)});
                Quote quote = quotes.get(String.valueOf(instrumentId));
                if (quote != null) {
                    price = quote.lastPrice;
                }
            } catch (IOException | KiteException e) {
                log.error("Error fetching quote for {}:{}", exchange, symbol, e);
            } catch (Exception e) {
                log.error("Error fetching instrument token for {}:{}", exchange, symbol, e);
                throw new RuntimeException(e);
            }
        }
        return price;
    }

    private int getMaxEligibleQuantity( String symbol, Double marginPrice) {
        int availableQuantity = 0;
        try {
            Margin userMargines = getUserMargin();
            double availableFunds =
                    Double.parseDouble(userMargines.available.liveBalance);
            if(marginPrice != null && marginPrice > 0) {
                // keep 25% margin for safety
                availableFunds = availableFunds * (Double)configManagerService.getOrDefaultValue(INTRADAY_MARGIN_THRESHOLD_KEY, INTRADAY_MARGIN_THRESHOLD);
                log.info("fund allocated for this stock: {}", availableFunds);
                availableQuantity = (int) (availableFunds / marginPrice);
                log.info("Max intraday quantity for symbol {}: {} (available funds: {}, price: {})", symbol, availableQuantity, availableFunds, marginPrice);
            } else {
                log.warn("Price not found for symbol: {}. Cannot calculate max intraday quantity.", symbol);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return availableQuantity;
    }

    public void createIntradayOrder(TradingSignal tradingSignal) {
        log.info("Creating intraday Orderby TradingIngine with details: {}", tradingSignal);
        try {
            String symbol = instrumentService.resolveSymbolByToken(tradingSignal.getInstrumentId());
            String cacheKey = generateOrderKey(symbol);
            SignalCacheHolder holder = getOrderFromCache(cacheKey, tradingSignal.getSignal().name(), symbol);
            if(holder.isDuplicateSignal()) {
                return;
            }
            OrderParams params = null;
            if(holder.isReturnOrder()) {
                params = holder.getParams();
            }else {
                TradingViewSignal viewSignal = new TradingViewSignal();
                viewSignal.setSymbol(symbol);
                viewSignal.setAction(tradingSignal.getSignal().name());
                viewSignal.setPrice(tradingSignal.getEntry());
                params = createOrderRequest(viewSignal);
            }
            OrderResponse order = placeZerodhaOrder(params, holder.isReturnOrder(), cacheKey);
            saveOrderData(order, params);
        } catch (Exception e) {
            log.error("Error placing order: {}", e.getMessage(), e);
        }
    }


    private OrderResponse placeZerodhaOrder(OrderParams params, boolean isReturnOrder, String cacheKey) {
        OrderResponse order = new OrderResponse();
        try {
            Boolean placeBuyOrder = (Boolean)configManagerService.getOrDefaultValue(ENABLE_TO_PLACE_BUY_ORDER_KEY, false);
            Boolean placeSellOrder = (Boolean)configManagerService.getOrDefaultValue(ENABLE_TO_PLACE_SELL_ORDER_KEY, false);
            if((params.transactionType.equalsIgnoreCase("BUY") && placeBuyOrder) ||
                    (params.transactionType.equalsIgnoreCase("SELL") && placeSellOrder)) {
                Set<String> watchList = configManagerService.getWatchlistForAction(params.transactionType);
                if(!watchList.isEmpty() && watchList.contains(params.tradingsymbol)) {
                    log.info("Placing wishList Order......");
                    log.info("Placing order: exchange={}, symbol={}, action={}, quantity={}, price={}",
                            params.exchange, params.tradingsymbol, params.transactionType, params.quantity, params.price);
                    order = kite.placeOrder(params, Constants.VARIETY_REGULAR);
                    log.info("Order placed successfully: orderId={}", order.orderId);
                }else {
                    log.info("given symbol={} not listed in wishList, hence skipping for order creation", params.tradingsymbol);
                }
            } else {
                log.info("Order placed Service is disabled for exchange={}, symbol={}, action={}",
                        params.exchange, params.tradingsymbol, params.transactionType);
            }
            if(!isReturnOrder) {
                saveOrderToCache(cacheKey, params);
            }

        }catch (IOException | KiteException e) {
            log.error("Error placing order: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to place an order", e);
        }
        return order;
    }


    public HistoricalData getHistoricalData(long instrumentId) {
        try {
            Integer backoutTimePeriod = (Integer) configManagerService.getOrDefaultValue(BACKOUT_TIME_PERIOD_KEY, BACKOUT_TIME_PERIOD);
            String timeInterval = (String) configManagerService.getOrDefaultValue(TIME_INTERVAL_KEY, TIME_INTERVAL);
            // Get enough historical data
            Calendar cal = Calendar.getInstance();
            Date to = cal.getTime();
            cal.add(Calendar.MINUTE, -backoutTimePeriod);
            Date from = cal.getTime();
            return kite.getHistoricalData(from, to, String.valueOf(instrumentId), timeInterval, false, false);
        } catch (IOException | KiteException e) {
            log.error("Error while fetching historic data", e);
            throw new KiteClientException(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching instrument token for  {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    private String saveOrderData(OrderResponse order, OrderParams params) {
        String prefix = "Test:";
        Random random = new Random();
        OrderData data = new OrderData();
        data.setOrderId(order.orderId != null ? order.orderId : prefix + System.currentTimeMillis() + random.nextInt(12));
        data.setAction(params.transactionType);
        data.setExchange(params.exchange);
        data.setSymbol(params.tradingsymbol);
        data.setQuantity(params.quantity);
        data.setPrice(params.price);
        data.setTimestamp(currentTime());
        marketDataService.saveOrderData(data);
        log.info("Order data saved successfully for OrderId: {}", data.getOrderId());
        return data.getOrderId();
    }


    private LocalDateTime currentTime() {
        return LocalDateTime.now();
    }

    public Margin getUserMargin() {
        try {
            return kite.getMargins("equity");
        } catch (KiteException e) {
            throw new KiteClientException(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Trade> getTrades() {
        try {
           return  kite.getTrades();
        } catch (KiteException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Order> getOrders() {
        try {
            return  kite.getOrders();
        } catch (KiteException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
