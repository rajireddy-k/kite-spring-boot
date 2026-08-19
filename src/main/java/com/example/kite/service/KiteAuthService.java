package com.example.kite.service;

import com.example.kite.config.KiteProperties;
import com.example.kite.dto.KiteStatusResponse;
import com.example.kite.dto.UserData;
import com.example.kite.entity.KiteSessionEntity;
import com.example.kite.repository.KiteSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class KiteAuthService {

    private final StringRedisTemplate redis;
    private final KiteConnect kite;
    private final KiteProperties properties;
    private final KiteSessionRepository sessionRepository;
    private final KiteTickerService tickerService;
    private final ObjectMapper objectMapper;

    public String loginUrl() {
        return kite.getLoginURL();
    }

    @Transactional
    public String handleCallback(String requestToken) throws Exception {
        log.info("Handling callback with request token: {}", requestToken);
        User user = null;
        try {
            user = kite.generateSession(requestToken, properties.getApiSecret());
            UserData userData = new UserData(user.userId, requestToken, user.accessToken, user.publicToken);
            saveToken(userData, kite.getApiKey());
        } catch (KiteException e) {
            throw new RuntimeException(e);
        }

        kite.setAccessToken(user.accessToken);
        kite.setPublicToken(user.publicToken);

        KiteSessionEntity session = sessionRepository.findById(1L)
                .orElseGet(KiteSessionEntity::new);

        session.setUserId(user.userId);
        session.setAccessToken(user.accessToken);
        session.setPublicToken(user.publicToken);
        session.setLoginTime(OffsetDateTime.now(ZoneOffset.ofHoursMinutes(5, 30)));
        session.setUpdatedAt(OffsetDateTime.now(ZoneOffset.ofHoursMinutes(5, 30)));

        sessionRepository.save(session);

        tickerService.start(user.accessToken);

        return user.userId;
    }

    public boolean authenticated() {
        return sessionRepository.existsById(1L);
    }

    public Optional<KiteSessionEntity> currentSession() {
        return sessionRepository.findById(1L);
    }

    public KiteStatusResponse status() {
        boolean isAuthenticated = authenticated();
        boolean isConnected = tickerService.isConnected();
        log.info("Checking status: authenticated={}, connected={}", isAuthenticated, isConnected);
        return new KiteStatusResponse(
                isAuthenticated,
                currentSession().map(KiteSessionEntity::getUserId).orElse(null),
                isConnected
        );
    }

    private void saveToken(UserData user, String apiKey) {
        try {
            String json = objectMapper.writeValueAsString(user);
            redis.opsForValue().set(apiKey, json);
        } catch (Exception e) {
            log.error("Unable to write user session to Redis", e);
        }
    }
}
