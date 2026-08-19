package com.example.kite.dto;


import java.time.LocalDateTime;
import java.time.OffsetDateTime;


public record TickMessage(
        Long id,
        String exchange,
        String symbol,
        long instrumentToken,
        double lastPrice,
        double changePercent,
        long volume,
        double lastTradedQuantity,
        double averagePrice,
        double openPrice,
        double highPrice,
        double lowPrice,
        double closePrice,
        double totalBuyQuantity,
        double totalSellQuantity,
        LocalDateTime timestamp
) {}
