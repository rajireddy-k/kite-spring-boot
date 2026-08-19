package com.example.kite.controller;


import com.example.kite.dto.KiteStatusResponse;
import com.example.kite.service.KiteAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;


import java.net.URI;
import java.util.Map;


@RestController
public class KiteAuthController {


    private final KiteAuthService authService;


    public KiteAuthController(KiteAuthService authService) {
        this.authService = authService;
    }


    @GetMapping("/api/kite/login")
    public RedirectView login() {
        //return ResponseEntity.ok(Map.of("loginUrl", authService.loginUrl()));
        return new RedirectView(authService.loginUrl());
    }


    @GetMapping("/api/kite/callback")
    public ResponseEntity<?> callback(
            @RequestParam("request_token") String requestToken) throws Exception {


        String userId = authService.handleCallback(requestToken);


        return ResponseEntity.status(302)
                .location(URI.create("/api/kite/status?login=success"))
                .body(Map.of("userId", userId));
    }


    @GetMapping("/api/kite/status")
    public KiteStatusResponse status() {
        return authService.status();
    }
}
