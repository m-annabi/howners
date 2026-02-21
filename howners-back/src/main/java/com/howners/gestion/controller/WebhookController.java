package com.howners.gestion.controller;

import com.howners.gestion.exception.esignature.WebhookValidationException;
import com.howners.gestion.service.contract.ContractESignatureService;
import com.howners.gestion.service.payment.PaymentService;
import com.howners.gestion.service.subscription.SubscriptionService;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
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

    private final ContractESignatureService esignatureService;
    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;

    /**
     * Webhook DocuSign
     *
     * POST /api/webhooks/docusign
     *
     * Reçoit les événements de DocuSign (signature complétée, refusée, vue, etc.)
     */
    @PostMapping("/docusign")
    public ResponseEntity<Void> handleDocuSignWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-DocuSign-Signature-1", required = false) String signature) {

        log.info("Received DocuSign webhook");

        try {
            esignatureService.processWebhook("docusign", payload, signature);
            return ResponseEntity.ok().build();
        } catch (WebhookValidationException e) {
            // HMAC validation failed - reject with 401 Unauthorized
            log.error("DocuSign webhook validation failed - rejecting request", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Error processing DocuSign webhook", e);
            // Return 500 for other errors (will cause DocuSign to retry)
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Webhook HelloSign (futur)
     *
     * POST /api/webhooks/hellosign
     */
    @PostMapping("/hellosign")
    public ResponseEntity<Void> handleHelloSignWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-HelloSign-Signature", required = false) String signature) {
        log.info("Received HelloSign webhook");

        try {
            esignatureService.processWebhook("hellosign", payload, signature);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to process HelloSign webhook", e);
            return ResponseEntity.ok().build();
        }
    }

    /**
     * Webhook Stripe (si déjà utilisé dans l'application)
     *
     * POST /api/webhooks/stripe
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        log.info("Received Stripe webhook");

        try {
            // Route subscription-related events to SubscriptionService
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

            // Process payment events
            paymentService.processStripeWebhook(payload, signature);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook processing failed");
        }
    }

    /**
     * Health check pour les webhooks
     *
     * GET /api/webhooks/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Webhooks endpoint is healthy");
    }
}
