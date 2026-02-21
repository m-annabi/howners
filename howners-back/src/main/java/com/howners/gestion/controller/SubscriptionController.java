package com.howners.gestion.controller;

import com.howners.gestion.dto.subscription.*;
import com.howners.gestion.service.subscription.FeatureGateService;
import com.howners.gestion.service.subscription.SubscriptionService;
import com.howners.gestion.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final FeatureGateService featureGateService;

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> getPlans() {
        log.info("Fetching all subscription plans");
        return ResponseEntity.ok(subscriptionService.getAllPlans());
    }

    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserSubscriptionResponse> getCurrentSubscription() {
        log.info("Fetching current subscription");
        UserSubscriptionResponse sub = subscriptionService.getCurrentSubscription();
        return ResponseEntity.ok(sub);
    }

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CheckoutSessionResponse> createCheckout(
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        log.info("Creating checkout session for plan: {}", request.planId());
        CheckoutSessionResponse response = subscriptionService.createCheckoutSession(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/billing-portal")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> createBillingPortal() {
        log.info("Creating billing portal session");
        String url = subscriptionService.createBillingPortalSession();
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelSubscription() {
        log.info("Cancelling subscription");
        subscriptionService.cancelSubscription();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/usage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UsageLimitsResponse> getUsageLimits() {
        var userId = AuthService.getCurrentUserId();
        return ResponseEntity.ok(featureGateService.getUsageLimits(userId));
    }
}
