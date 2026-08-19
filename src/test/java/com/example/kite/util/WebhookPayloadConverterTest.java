package com.example.kite.util;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WebhookPayloadConverterTest {

    private WebhookPayloadConverter source;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        objectMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
        );
    }

    @Test
    public void testValidStringpayload() {
        String payload = "Old school concept ( symbol: TCS , exchange: NSE, price: 2357.9 , buy: 0, sell: 1 )";
        WebhookPayloadConverter.convertStringPayload(payload, objectMapper);
    }

    @Test
    public void testValidJsonPayload() {
        String payload = """
                {
                    "source": "TradingView",
                    "event": "TradingViewSignal",
                    "action": "BUY",
                    "symbol": "NSE:RELIANCE",
                    "exchange": "NSE",
                    "timeframe": "15",
                    "price": "1425.50",
                    "timestamp": "1754022600000"
                }
                """;
        WebhookPayloadConverter.convertStringPayload(payload, objectMapper);
    }
}
