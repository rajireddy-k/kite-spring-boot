package com.example.kite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KiteMarketDataApplication {
    public static void main(String[] args) {
        SpringApplication.run(KiteMarketDataApplication.class, args);
    }
}
