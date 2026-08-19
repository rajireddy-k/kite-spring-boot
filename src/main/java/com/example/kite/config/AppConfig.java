package com.example.kite.config;

import com.zerodhatech.kiteconnect.KiteConnect;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(KiteProperties.class)
public class AppConfig {

    @Bean
    public KiteConnect kiteConnect(KiteProperties properties) {
        return new KiteConnect(properties.getApiKey());
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
