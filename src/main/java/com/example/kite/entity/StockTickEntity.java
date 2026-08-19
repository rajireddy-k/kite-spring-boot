package com.example.kite.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "stock_tick",
    indexes = {
        @Index(name = "idx_tick_symbol_time", columnList = "exchange,symbol,tickTime")
    }
)
@Data
public class StockTickEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String exchange;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private Long instrumentToken;

    @Column(nullable = false)
    private double lastPrice;

    private double changePercent;
    private long volume;
    private double lastTradedQuantity;
    private double averagePrice;
    private double openPrice;
    private double highPrice;
    private double lowPrice;
    private double closePrice;
    private double totalBuyQuantity;
    private double totalSellQuantity;
    private Timestamp tickTime;


}
