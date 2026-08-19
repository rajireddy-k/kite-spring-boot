package com.example.kite.dto;

import lombok.Data;

import java.util.List;

@Data
public class TradingSubscriptionRequest {
    String exchange;
    List<String> symbols;
}
