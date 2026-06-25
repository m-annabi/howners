package com.howners.gestion.controller;

import com.howners.gestion.exception.esignature.WebhookValidationException;
import com.howners.gestion.service.contract.ContractESignatureService;
import com.howners.gestion.service.payment.PaymentService;
import com.howners.gestion.service.subscription.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

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
     * Webhook Stripe (si déjà utilisé dans l'application)
     *
     * POST /api/webhooks/stripe
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        log.info("Received Stripe webhook");

        // Signature vérifiée AVANT tout traitement (un payload non signé est rejeté en prod).
        final Event event;
        try {
            event = constructStripeEvent(payload, signature);
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        try {
            // Événements d'abonnement -> SubscriptionService (à partir d'un événement vérifié)
            if (event.getType() != null && event.getType().startsWith("customer.subscription.")) {
                event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
                    if (obj instanceof Subscription sub) {
                        String priceId = null;
                        if (sub.getItems() != null && sub.getItems().getData() != null
                                && !sub.getItems().getData().isEmpty()
                                && sub.getItems().getData().get(0).getPrice() != null) {
                            priceId = sub.getItems().getData().get(0).getPrice().getId();
                        }
                        subscriptionService.processSubscriptionWebhook(
                                event.getType(),
                                sub.getId(),
                                sub.getCustomer(),
                                priceId,
                                sub.getCurrentPeriodStart(),
                                sub.getCurrentPeriodEnd()
                        );
                    }
                });
            }

            // Événements de paiement (processStripeWebhook revérifie la signature)
            paymentService.processStripeWebhook(payload, signature);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook processing failed");
        }
    }

    /**
     * Construit l'événement Stripe en vérifiant la signature quand un secret est
     * configuré ; en l'absence de secret (dev local) on retombe sur un parsing simple,
     * comme {@code PaymentService.processStripeWebhook}.
     */
    private Event constructStripeEvent(String payload, String signature) throws SignatureVerificationException {
        if (stripeWebhookSecret != null && !stripeWebhookSecret.isBlank()) {
            return Webhook.constructEvent(payload, signature, stripeWebhookSecret);
        }
        return Event.GSON.fromJson(payload, Event.class);
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
