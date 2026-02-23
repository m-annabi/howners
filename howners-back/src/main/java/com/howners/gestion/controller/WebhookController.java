package com.howners.gestion.controller;

import com.howners.gestion.service.payment.PaymentService;
import com.howners.gestion.service.subscription.SubscriptionService;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller pour recevoir les webhooks des fournisseurs externes
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;

    /**
     * Webhook Stripe
     *
     * POST /api/webhooks/stripe
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        log.info("Received Stripe webhook");

        try {
            if (payload.contains("customer.subscription.")) {
                try {
                    Event event = Event.GSON.fromJson(payload, Event.class);
                    String eventType = event.getType();

                    event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
                        if (obj instanceof Subscription sub) {
                            subscriptionService.processSubscriptionWebhook(
                                    eventType,
                                    sub.getId(),
                                    sub.getCustomer(),
                                    sub.getCurrentPeriodStart(),
                                    sub.getCurrentPeriodEnd()
                            );
                        }
                    });
                } catch (Exception subEx) {
                    log.error("Error processing subscription webhook: {}", subEx.getMessage());
                }
            }

            paymentService.processStripeWebhook(payload, signature);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook processing failed");
        }
    }

    /**
     * Health check pour les webhooks
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Webhooks endpoint is healthy");
    }
}
