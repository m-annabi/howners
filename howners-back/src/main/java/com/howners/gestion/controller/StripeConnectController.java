package com.howners.gestion.controller;

import com.howners.gestion.dto.payments.StripeConnectStatusResponse;
import com.howners.gestion.service.payments.StripeConnectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe-connect")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class StripeConnectController {

    private final StripeConnectService stripeConnectService;

    @GetMapping("/status")
    public ResponseEntity<StripeConnectStatusResponse> status() {
        return ResponseEntity.ok(stripeConnectService.getStatus());
    }

    @PostMapping("/onboarding")
    public ResponseEntity<StripeConnectStatusResponse> onboarding() {
        return ResponseEntity.ok(stripeConnectService.createOrRefreshOnboarding());
    }
}
