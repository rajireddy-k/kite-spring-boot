package com.example.kite.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


import java.sql.Date;
import java.sql.Timestamp;
import java.time.OffsetDateTime;


@Entity
@Table(name = "kite_trading_signal")
@Data
public class TradingSignalEntity {


    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private String symbol;


    @Column(nullable = false)
    private String action;


    private double entry;


    private double target;


    private double stopLoss;


    private double risk;


    private double riskReward;


    private double momentum;


    private double atr;


    private double averageClose;


    private double resistance;


    private double support;


    private String reason;


    private Timestamp timestamp;


    private String source;
}
