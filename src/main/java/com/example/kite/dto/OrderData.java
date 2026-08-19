package com.example.kite.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class OrderData {

    private String orderId;
    private Double price;
    private int quantity;
    private String action;
    private String exchange;
    private String symbol;
    private LocalDateTime timestamp;
}
