package com.example.kite.controller;


import com.example.kite.service.OfflineTradingService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@AllArgsConstructor
public class OfflineTradingController {


    private final OfflineTradingService offlineTradingService;


    @PostMapping("api/trading/offline/{exchange}/{symbol}")
    public ResponseEntity<String> processOfflineTrading(@PathVariable("exchange") String exchange, @PathVariable("symbol") String symbol) {
        offlineTradingService.performOfflineTrading(exchange, symbol);
        return new ResponseEntity<>("Processor executed successfully, verify the db report for trading orders", HttpStatus.OK);
    }
}
