package com.example.kite.controller;


import com.example.kite.dto.*;
import com.example.kite.scheduler.PriceChangeAlertScheduler;
import com.example.kite.service.*;
import com.zerodhatech.models.HistoricalData;
import com.zerodhatech.models.Margin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/market")
@Slf4j
public class MarketDataController {


    private final InstrumentService instrumentService;
    private final KiteTickerService tickerService;
    private final MarketDataService marketDataService;
    private final PriceChangeAlertScheduler priceChangeAlertScheduler;
    private final ZerodhaService zerodhaService;
    private final KiteMarginCalculator marginCalculator;


    public MarketDataController(
            InstrumentService instrumentService,
            KiteTickerService tickerService,
            MarketDataService marketDataService,
            PriceChangeAlertScheduler priceChangeAlertScheduler,
            ZerodhaService zerodhaService,
            KiteMarginCalculator marginCalculator) {
        this.instrumentService = instrumentService;
        this.tickerService = tickerService;
        this.marketDataService = marketDataService;
        this.priceChangeAlertScheduler = priceChangeAlertScheduler;
        this.zerodhaService = zerodhaService;
        this.marginCalculator = marginCalculator;
    }


    @PostMapping("/subscribe/{exchange}/{symbol}")
    public ResponseEntity<?> subscribe(
            @PathVariable String exchange,
            @PathVariable String symbol) throws Exception {

        long token = instrumentService.tokenFor(exchange, symbol);
        marketDataService.register(token, exchange, symbol);
        tickerService.subscribe(token);
        //priceChangeAlertScheduler.subscribeSymbol(symbol, exchange);

        return ResponseEntity.ok(Map.of(
                "exchange", exchange.toUpperCase(),
                "symbol", symbol.toUpperCase(),
                "instrumentToken", token,
                "subscribed", true
        ));
    }

    @PostMapping("/subscribe/bulk/{exchange}")
    public ResponseEntity<StocktickSubscriptionResponse> subscribeBulk(@PathVariable String exchange,
                                           @RequestBody TradingSubscriptionRequest subscriptionRequest) {
        List<String> symbols = subscriptionRequest.getSymbols();
        List<StocktickSubscription> subscriptionList = symbols.stream().map(
                symbol-> {
                    try {
                        long token = instrumentService.tokenFor(exchange, symbol);
                        marketDataService.register(token, exchange, symbol);
                        tickerService.subscribe(token);
                        StocktickSubscription subscription = new StocktickSubscription();
                        subscription.setInstrumentToken(token);
                        subscription.setSymbol(symbol);
                        return subscription;
                    } catch (Exception e) {
                        log.error("Exception while subscribing symbol={}", symbol, e);
                        return null;
                    }
                }
        ).toList();
        StocktickSubscriptionResponse response = new StocktickSubscriptionResponse();
        response.setSubscriptions(subscriptionList);
        response.setExchange(exchange);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/subscribe/{exchange}/{symbol}")
    public ResponseEntity<?> unsubscribe(
            @PathVariable String exchange,
            @PathVariable String symbol) throws Exception {

        long token = instrumentService.tokenFor(exchange, symbol);
        tickerService.unsubscribe(token);

        return ResponseEntity.ok(Map.of("unsubscribed", true));
    }

    @PostMapping("/unsubscribe/bulk/{exchange}")
    public ResponseEntity<StocktickSubscriptionResponse> unsubscribeBulk(@PathVariable String exchange,
                                                                       @RequestBody TradingSubscriptionRequest subscriptionRequest) {
        List<String> symbols = subscriptionRequest.getSymbols();
        List<StocktickSubscription> subscriptionList = symbols.stream().map(
                symbol-> {
                    try {
                        long token = instrumentService.tokenFor(exchange, symbol);
                        tickerService.unsubscribe(token);
                        StocktickSubscription subscription = new StocktickSubscription();
                        subscription.setInstrumentToken(token);
                        subscription.setSymbol(symbol);
                        return subscription;
                    } catch (Exception e) {
                        log.error("Exception while subscribing symbol={}", symbol, e);
                        return null;
                    }
                }
        ).toList();
        StocktickSubscriptionResponse response = new StocktickSubscriptionResponse();
        response.setSubscriptions(subscriptionList);
        response.setExchange(exchange);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{exchange}/{symbol}")
    public ResponseEntity<?> latest(
            @PathVariable String exchange,
            @PathVariable String symbol) {


        String json = marketDataService.latest(exchange, symbol);


        if (json == null) {
            return ResponseEntity.notFound().build();
        }


        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(json);
    }


    @GetMapping("histarical/{exchange}/{symbol}")
    public ResponseEntity<?> histaricalCandle(@PathVariable String exchange, @PathVariable String symbol) {


        Long instrumentToken = null;
        try {
            instrumentToken = instrumentService.tokenFor(exchange, symbol);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
        com.zerodhatech.models.HistoricalData data = zerodhaService.getHistoricalData(instrumentToken);


        if (data == null) {
            return ResponseEntity.notFound().build();
        }


        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(data);
    }
    @GetMapping("/margin/{exchange}/{symbol}")
    public ResponseEntity<MarginResult> getUserMarginData(@PathVariable String exchange,
                                                          @PathVariable String symbol,
                                                          @RequestParam("transactiontype") String transactionType,
                                                          @RequestParam("quantity") Integer quantity,
                                                          @RequestParam("price") Double price) {

        MarginResult result = marginCalculator.calculateMISMargin(exchange, symbol,transactionType, quantity, price);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/user/margin")
    public ResponseEntity<Margin> getUserMarginData() {
        Margin result = zerodhaService.getUserMargin();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/topTrendingStocks")
    public ResponseEntity<?> topTendingIntradayStock() {
        List<TickMessage> stockTicks = marketDataService.findListOfPreferredSymols();
        if(!stockTicks.isEmpty()) {
            log.info("Found Top picked stocks for today trading :", stockTicks.size());
            Map<Boolean, List<TickMessage>> partitioned =
                    stockTicks.stream()
                            .collect(Collectors.partitioningBy(
                                    tick -> tick.openPrice() == tick.lowPrice()
                            ));
            Map<String, List<TickMessage>> topTrendingStocks = new HashMap<>();
            topTrendingStocks.put("Bullish", partitioned.get(true));
            topTrendingStocks.put("Bearish", partitioned.get(false));
            return new ResponseEntity<>(topTrendingStocks, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/tradeList")
    public ResponseEntity<?> getAllTrades() {
        return new ResponseEntity<>(zerodhaService.getTrades(), HttpStatus.OK);
    }

    @GetMapping("/orderList")
    public ResponseEntity<?> getAllOrders() {
        return new ResponseEntity<>(zerodhaService.getOrders(), HttpStatus.OK);
    }
}
