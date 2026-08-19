package com.example.kite.util;

import com.example.kite.dto.TradingViewSignal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
public class WebhookPayloadConverter {

    private static final String SYMBOL = "symbol";
    private static final String EXCHAGE = "exchange";
    private static final String PRICE = "price";
    private static final String BUY = "isBuy";
    private static final String SELL = "isSell";
    private static final String OPEN_PRICE = "open";
    private static final String HIGH_PRICE = "high";
    private static final String LOW_PRICE = "low";
    private static final String VOLUME = "volume";

    public static TradingViewSignal convertStringPayload(String payload, ObjectMapper objectMapper) {
        TradingViewSignal signalObject = null;
        if(payload.contains("(")) {
            String source = payload.substring(0, payload.indexOf("("));
            String data = payload.substring(payload.indexOf("(")+1, payload.indexOf(")"));
            String [] values = data.split(",");
            if(values.length>0){
                Map<String, String> signalData = new HashMap<>();
                for(String pair : values) {
                    String [] keyValuePair = pair.trim().split(":");
                    signalData.put(keyValuePair[0].trim(), keyValuePair[1].trim());
                }
                if(!signalData.isEmpty()) {
                    signalObject = new TradingViewSignal();
                    signalObject.setSource(source.trim());
                    boolean isBuy = Integer.valueOf(nullsafeGet(BUY, signalData).intValue()) == 1;
                    boolean isSell = Integer.valueOf(nullsafeGet(SELL, signalData).intValue()) == 1;

                    signalObject.setAction(isBuy?"BUY":isSell?"SELL":"NONE");
                    signalObject.setPrice(nullsafeGet(PRICE, signalData));
                    signalObject.setSymbol(signalData.get(EXCHAGE)+":"+signalData.get(SYMBOL));
                    signalObject.setMarketSymbol(signalData.get(SYMBOL));
                    signalObject.setQuantity(1);
                    signalObject.setHigh(nullsafeGet(HIGH_PRICE, signalData));
                    signalObject.setLow(nullsafeGet(LOW_PRICE, signalData));
                    signalObject.setOpen(nullsafeGet(OPEN_PRICE, signalData));
                    signalObject.setVolume(nullsafeGet(VOLUME, signalData));
                }
            }
        } else {
            try {
                signalObject = objectMapper.readValue(payload, TradingViewSignal.class);
            } catch (JsonProcessingException e) {
                log.error("Error parsing TradingView signal: {}", e.getMessage());
                return null;
            }
        }
        return signalObject;
    }

    private static Double nullsafeGet(String key,  Map<String, String> signalData) {
        String value =signalData.get(key);
        if(hasText(value)) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
               log.error("Invalid value for key={}", key,  e) ;
            }
        }
        return 0.0;
    }
}
