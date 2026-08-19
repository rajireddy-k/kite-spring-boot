package com.example.kite.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Table(name = "kite_candle")
@Data
public class CandleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String symbol;
    @Column(nullable = false)
    private OffsetDateTime timestamp;
    @Column(nullable = false)
    private double open;
    @Column(nullable = false)
    private double high;
    @Column(nullable = false)
    private double low;
    @Column(nullable = false)
    private double close;
}
