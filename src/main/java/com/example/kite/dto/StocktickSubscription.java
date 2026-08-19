package com.example.kite.dto;

import lombok.Data;

@Data
public class StocktickSubscription {
    private long instrumentToken;
    private String symbol;
}
