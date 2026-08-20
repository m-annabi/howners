package com.howners.gestion.controller;

import com.howners.gestion.exception.esignature.WebhookValidationException;
import com.howners.gestion.service.contract.ContractESignatureService;
import com.howners.gestion.service.payment.PaymentService;
import com.howners.gestion.service.payments.StripeConnectService;
import com.howners.gestion.service.subscription.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
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
    private final StripeConnectService stripeConnectService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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
            // Abonnements : on lit le JSON BRUT du payload (getRawJson) plutôt que l'objet typé.
            // getObject() renvoie vide quand la version d'API de l'événement diffère de celle du
            // SDK, et depuis l'API 2025+ la période est portée par la ligne d'abonnement (non
            // typée dans le SDK) : le JSON brut est la source fiable, indépendante de ces écarts.
            if (event.getType() != null && event.getType().startsWith("customer.subscription.")) {
                handleSubscriptionEvent(event);
            }

            // Statut des comptes Connect (bailleurs) -> StripeConnectService
            if ("account.updated".equals(event.getType())
                    && resolveEventObject(event) instanceof Account account) {
                stripeConnectService.processAccountUpdate(
                        account.getId(), account.getChargesEnabled(), account.getPayoutsEnabled());
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
     * Résout l'objet métier d'un événement Stripe. {@code getObject()} renvoie vide lorsque la
     * version d'API de l'événement diffère de celle épinglée dans le SDK (le webhook est
     * volontairement sur une version récente) ; on force alors {@code deserializeUnsafe()} pour
     * traiter l'événement au lieu de l'ignorer silencieusement.
     */
    private StripeObject resolveEventObject(Event event) {
        return event.getDataObjectDeserializer().getObject().orElseGet(() -> {
            try {
                return event.getDataObjectDeserializer().deserializeUnsafe();
            } catch (com.stripe.exception.EventDataObjectDeserializationException e) {
                log.error("Impossible de désérialiser l'objet de l'événement Stripe {}", event.getType(), e);
                return null;
            }
        });
    }

    /**
     * Traite un événement d'abonnement à partir du JSON brut du payload. On extrait
     * l'identifiant d'abonnement, le client, le price et la période. La période provient de
     * l'abonnement (API historique) ou, à défaut, de la 1re ligne d'abonnement (API 2025+),
     * où Stripe l'a déplacée.
     */
    private void handleSubscriptionEvent(Event event) {
        String rawJson = event.getDataObjectDeserializer().getRawJson();
        if (rawJson == null || rawJson.isBlank()) {
            log.warn("Événement d'abonnement {} sans données JSON exploitables", event.getType());
            return;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode sub = objectMapper.readTree(rawJson);
            String subscriptionId = asText(sub.get("id"));
            String customerId = asText(sub.get("customer"));
            Long periodStart = asLong(sub.get("current_period_start"));
            Long periodEnd = asLong(sub.get("current_period_end"));
            String priceId = null;

            com.fasterxml.jackson.databind.JsonNode item = sub.path("items").path("data").path(0);
            if (!item.isMissingNode()) {
                priceId = asText(item.path("price").get("id"));
                if (periodStart == null) periodStart = asLong(item.get("current_period_start"));
                if (periodEnd == null) periodEnd = asLong(item.get("current_period_end"));
            }

            subscriptionService.processSubscriptionWebhook(
                    event.getType(), subscriptionId, customerId, priceId, periodStart, periodEnd);
        } catch (Exception e) {
            log.error("Échec du traitement de l'événement d'abonnement {}", event.getType(), e);
        }
    }

    private static String asText(com.fasterxml.jackson.databind.JsonNode node) {
        return node != null && !node.isNull() ? node.asText() : null;
    }

    private static Long asLong(com.fasterxml.jackson.databind.JsonNode node) {
        return node != null && node.isNumber() ? node.asLong() : null;
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
