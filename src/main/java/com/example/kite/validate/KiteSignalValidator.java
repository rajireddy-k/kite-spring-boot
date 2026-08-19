package com.example.kite.validate;

import com.example.kite.dto.TradingViewSignal;
import com.example.kite.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KiteSignalValidator {

    @Value("${tradingview.webhook-secret}")
    private String webhookSecret;

    public boolean isValidSignal(TradingViewSignal signal, String webhookToken) {
        if(signal == null) {
            throw new ValidationException("Invalid payload");
        }
        // 1. Authenticate webhook
        String webhookSecretOnRequest = webhookToken;
        if(webhookSecretOnRequest == null ) {
            webhookSecretOnRequest = signal.getSecret();
        }
        if (!webhookSecret.equals(webhookSecretOnRequest)) {
            throw new ValidationException("Invalid webhook secret");
        }

        // 2. Validate action
        if (signal.getAction() == null ) {
            throw new ValidationException("Invalid action");
        }

        // 4. Validate required fields
        if (signal.getSymbol() == null ) {
            throw new ValidationException("Missing order fields");
        }


        return true;
    }
}
