package com.example.kite.trading.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "trading.strategy")
public class StrategyProperties {
    private int emaFastPeriod = 20;
    private int emaSlowPeriod = 50;
    private int atrPeriod = 14;
    private int rsiPeriod = 14;
    private int volumeSmaPeriod = 20;


    private double minimumAtr = 0.50;
    private double buyRsiThreshold = 55.0;
    private double sellRsiThreshold = 45.0;


    private double stopLossAtrMultiplier = 1.5;
    private double riskRewardRatio = 2.0;


    private boolean resetVwapOnSessionChange = true;
}
