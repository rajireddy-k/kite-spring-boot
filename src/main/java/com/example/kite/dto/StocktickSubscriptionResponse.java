package com.example.kite.dto;

import lombok.Data;

import java.util.List;

@Data
public class StocktickSubscriptionResponse {
    private String exchange;
    private List<StocktickSubscription> subscriptions;
}
