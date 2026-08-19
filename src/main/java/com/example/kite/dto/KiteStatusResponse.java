package com.example.kite.dto;

public record KiteStatusResponse(
        boolean authenticated,
        String userId,
        boolean tickerConnected
) {}
