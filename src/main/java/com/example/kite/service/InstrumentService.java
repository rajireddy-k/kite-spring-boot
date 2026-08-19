package com.example.kite.service;


import com.example.kite.exception.KiteClientException;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Instrument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
public class InstrumentService {


    private final KiteConnect kite;
    private final Map<String, Long> cache = new ConcurrentHashMap<>();
    private final Map<Long, String> resolveSymbol = new ConcurrentHashMap<>();


    public InstrumentService(KiteConnect kite) {
        this.kite = kite;
    }


    public long tokenFor(String exchange, String symbol) throws Exception {
        log.info("Fetching token for exchange: {}, symbol: {}", exchange, symbol);
        String key = exchange.toUpperCase() + ":" + symbol.toUpperCase();
        Long cached = cache.get(key);
        if (cached != null) {
            resolveSymbol.putIfAbsent(cached, key);
            return cached;
        }


        List<Instrument> instruments = null;
        try {
            instruments = kite.getInstruments(exchange.toUpperCase());
        } catch (KiteException e) {
            log.error("Error fetching instruments from Kite API: {}", e.getMessage());
            throw new KiteClientException(e.getMessage());
        }


        for (Instrument instrument : instruments) {
            if (exchange.equalsIgnoreCase(instrument.getExchange())
                    && symbol.equalsIgnoreCase(instrument.getTradingsymbol())) {
                long token = instrument.getInstrument_token();
                cache.put(key, token);
                resolveSymbol.putIfAbsent(token, key);
                return token;
            }
        }
        log.error("Instrument not found for exchange: {}, symbol: {}", exchange, symbol);
        throw new IllegalArgumentException("Instrument not found: " + key);
    }


    public String resolveSymbolByToken(Long instrumentId){
        return resolveSymbol.get(instrumentId);
    }
}
