package com.example.kite.controller;


import com.example.kite.dto.TradingViewSignal;
import com.example.kite.enums.SignalSourceType;
import com.example.kite.service.ZerodhaService;
import com.example.kite.util.AlertSoundListener;
import com.example.kite.util.WebhookPayloadConverter;
import com.example.kite.validate.KiteSignalValidator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/webhook")
@Slf4j
public class TradingWebhookController {


    private final ZerodhaService zerodhaService;


    private final KiteSignalValidator kiteSignalValidator;


    private final ObjectMapper objectMapper;


    public TradingWebhookController(ZerodhaService zerodhaService,
                                    KiteSignalValidator kiteSignalValidator,
                                    ObjectMapper objectMapper) {
        this.zerodhaService = zerodhaService;
        this.kiteSignalValidator = kiteSignalValidator;
        this.objectMapper = objectMapper;
        objectMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
        );
    }


    @PostMapping("/tradingview/webhook")
    public ResponseEntity<?> receiveSignal(@RequestParam("webhook_token") String webhookToken,
                                           @RequestBody String signal) {
        log.info("Received TradingView signal: {}", signal);
        Thread.startVirtualThread(()->AlertSoundListener.beep(SignalSourceType.WEBHOOK));
        TradingViewSignal signalObject = WebhookPayloadConverter.convertStringPayload(signal, objectMapper);

        kiteSignalValidator.isValidSignal(signalObject, webhookToken);
        Thread.startVirtualThread(() -> zerodhaService.placeOrder(signalObject));
        //String orderId = zerodhaService.placeOrder(signalObject);
        log.info("Processed TradingView signal: {}", signal);
        return ResponseEntity.ok("Order submitted: ");
    }
}
