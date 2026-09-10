package com.howners.gestion.service.payment;

import com.howners.gestion.service.rental.RentalAccessService;
import com.howners.gestion.service.notification.NotificationDispatcher;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.payment.PaymentType;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.GenericNotificationEmailData;
import com.howners.gestion.dto.email.PaymentReminderEmailData;
import com.howners.gestion.dto.payment.CreatePaymentRequest;
import com.howners.gestion.dto.payment.PaymentResponse;
import com.howners.gestion.dto.payment.StripePaymentIntentResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.service.audit.AuditService;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.notification.NotificationService;
import com.howners.gestion.service.receipt.ReceiptService;
import com.howners.gestion.domain.notification.NotificationType;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.ApiResource;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.howners.gestion.dto.subscription.CheckoutSessionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ReceiptService receiptService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final PlatformFeeService platformFeeService;
    private final RentalAccessService rentalAccessService;
    private final NotificationDispatcher notificationDispatcher;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Value("${stripe.connect-webhook-secret:}")
    private String stripeConnectWebhookSecret;

    @Value("${stripe.api-key:}")
    private String stripeApiKey;

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public List<PaymentResponse> findByCurrentUser() {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        if (currentUser.getRole() == Role.ADMIN) {
            return paymentRepository.findAll().stream()
                    .map(PaymentResponse::from)
                    .collect(Collectors.toList());
        }

        if (currentUser.getRole() == Role.TENANT) {
            return paymentRepository.findByPayerId(currentUserId).stream()
                    .map(PaymentResponse::from)
                    .collect(Collectors.toList());
        }

        return paymentRepository.findByOwnerId(currentUserId).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(UUID paymentId) {
        Payment payment = findPaymentAndCheckAccess(paymentId);
        BigDecimal feePercent = platformFeeService.getFeePercentPourProprietaire(
                payment.getRental().getProperty().getOwner().getId());
        return PaymentResponse.from(payment, feePercent);
    }

    /**
     * Le locataire déclare avoir réglé hors plateforme (virement, chèque, espèces). Le paiement
     * reste en attente jusqu'à la confirmation de réception par le bailleur, qui est prévenu.
     */
    @Transactional
    public PaymentResponse declarePayment(UUID paymentId, String method) {
        Payment payment = findPaymentAndCheckAccess(paymentId);
        if (!payment.getPayer().getId().equals(AuthService.getCurrentUserId())) {
            throw new ForbiddenException("Seul le locataire concerné peut déclarer ce règlement.");
        }
        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.LATE) {
            throw new BadRequestException("Ce paiement n'est pas en attente de règlement.");
        }
        String declaredMethod = method == null || method.isBlank() ? "BANK_TRANSFER" : method.trim().toUpperCase();
        payment.setDeclaredAt(LocalDateTime.now());
        payment.setDeclaredMethod(declaredMethod);
        payment = paymentRepository.save(payment);
        auditService.logAction(AuditAction.UPDATE, "Payment", payment.getId());

        User owner = payment.getRental().getProperty().getOwner();
        String label = switch (declaredMethod) {
            case "CHECK" -> "par chèque";
            case "CASH" -> "en espèces";
            default -> "par virement";
        };
        String amountLabel = payment.getAmount() + " " + payment.getCurrency();
        notificationDispatcher.notifyAndEmail(owner, NotificationType.PAYMENT_DUE,
                "Règlement déclaré par votre locataire",
                payment.getPayer().getFullName() + " déclare avoir réglé " + amountLabel + " " + label
                        + " pour " + payment.getRental().getProperty().getName() + ". Confirmez la réception.",
                "/payments/" + payment.getId(),
                new NotificationDispatcher.Email(
                        "Règlement déclaré — " + payment.getRental().getProperty().getName(),
                        "Règlement déclaré par votre locataire",
                        payment.getPayer().getFullName() + " indique avoir réglé <strong>" + amountLabel + "</strong> " + label
                                + ". Vérifiez votre compte puis confirmez la réception : la quittance sera générée automatiquement.",
                        "<strong>Bien :</strong> " + payment.getRental().getProperty().getName()
                                + (payment.getDueDate() != null ? "<br><strong>Échéance :</strong> " + payment.getDueDate() : ""),
                        "Confirmer la réception",
                        frontendUrl + "/payments/" + payment.getId(),
                        false));
        log.info("Payment {} declared as paid ({}) by tenant {}", payment.getId(), declaredMethod, payment.getPayer().getId());
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findByRentalId(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));
        rentalAccessService.assertParticipant(rental);
        return paymentRepository.findByRentalId(rentalId).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        Rental rental = rentalRepository.findById(request.rentalId())
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", request.rentalId().toString()));

        // Only the property owner can create payments
        rentalAccessService.assertOwner(rental, "You are not authorized to create payments for this rental");

        User payer = rental.getTenant();
        if (payer == null) {
            throw new BadRequestException("Cannot create payment: no tenant assigned to this rental");
        }

        if (request.paymentType() == PaymentType.RENT && request.dueDate() != null) {
            LocalDate monthStart = request.dueDate().withDayOfMonth(1);
            if (paymentRepository.existsActiveRentPaymentInMonth(rental.getId(), monthStart, monthStart.plusMonths(1))) {
                throw new BadRequestException("Un paiement de loyer existe déjà pour ce bail sur le mois de l'échéance");
            }
            if (rental.getStartDate() != null && request.dueDate().isBefore(rental.getStartDate().withDayOfMonth(1))) {
                throw new BadRequestException("L'échéance est antérieure au début du bail (" + rental.getStartDate() + ")");
            }
            if (rental.getEndDate() != null && request.dueDate().isAfter(rental.getEndDate())) {
                throw new BadRequestException("L'échéance est postérieure à la fin du bail (" + rental.getEndDate() + ")");
            }
        }

        Payment payment = Payment.builder()
                .rental(rental)
                .payer(payer)
                .paymentType(request.paymentType())
                .amount(request.amount())
                .currency(request.currency() != null ? request.currency() : "EUR")
                .status(PaymentStatus.PENDING)
                .dueDate(request.dueDate())
                .paymentMethod(request.paymentMethod())
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment created with id {} for rental {}", payment.getId(), rental.getId());
        auditService.logAction(AuditAction.PAYMENT_CREATED, "Payment", payment.getId());

        notifyTenantOfNewPayment(payment);

        return PaymentResponse.from(payment);
    }

    /** Notifie immédiatement le locataire (in-app + email) qu'une échéance vient d'être créée. */
    private void notifyTenantOfNewPayment(Payment payment) {
        try {
            Rental rental = payment.getRental();
            Property property = rental.getProperty();
            User tenant = payment.getPayer();
            User owner = property.getOwner();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String amountLabel = payment.getAmount() + " " + payment.getCurrency();
            String dueDateLabel = payment.getDueDate() != null ? payment.getDueDate().format(dateFormatter) : null;

            String detailsHtml = "<strong>Bien :</strong> " + property.getName() + "<br>"
                    + "<strong>Montant :</strong> " + amountLabel
                    + (dueDateLabel != null ? "<br><strong>Échéance :</strong> " + dueDateLabel : "");

            notificationDispatcher.notifyAndEmail(tenant, NotificationType.PAYMENT_DUE,
                    "Nouvelle échéance de paiement",
                    "Un paiement de " + amountLabel
                            + (dueDateLabel != null ? " à régler avant le " + dueDateLabel : " à régler")
                            + " a été enregistré par " + owner.getFullName() + ".",
                    "/payments/" + payment.getId(),
                    new NotificationDispatcher.Email(
                            "Nouvelle échéance de paiement — " + property.getName(),
                            "Nouvelle échéance de paiement",
                            "Votre propriétaire " + owner.getFullName() + " a enregistré une nouvelle échéance de paiement pour votre location.",
                            detailsHtml,
                            "Voir le paiement",
                            frontendUrl + "/payments/" + payment.getId(),
                            false));
        } catch (Exception e) {
            log.error("Échec de la notification de création de paiement {}: {}", payment.getId(), e.getMessage(), e);
        }
    }

    @Transactional
    public StripePaymentIntentResponse createStripePaymentIntent(UUID paymentId) {
        Payment payment = findPaymentAndCheckAccess(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Payment is not in PENDING status");
        }

        User owner = payment.getRental().getProperty().getOwner();
        String connectedAccountId = requireOnlinePaymentEnabled(owner);

        try {
            long amountInCents = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            BigDecimal platformFeePercent = platformFeeService.getFeePercentPourProprietaire(owner.getId());
            long platformFee = Math.round(amountInCents * platformFeePercent.doubleValue() / 100.0);

            // Direct charge : le PaymentIntent est créé DIRECTEMENT sur le compte Connect du
            // propriétaire (RequestOptions.setStripeAccount) — l'argent ne transite jamais par
            // le compte de la plateforme, qui ne fait que prélever sa commission au passage
            // (applicationFeeAmount). Pas de transfert/reversement à effectuer ensuite.
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(payment.getCurrency().toLowerCase())
                    .setApplicationFeeAmount(platformFee)
                    .putMetadata("payment_id", payment.getId().toString())
                    .putMetadata("rental_id", payment.getRental().getId().toString())
                    .build();
            RequestOptions options = RequestOptions.builder().setStripeAccount(connectedAccountId).build();

            PaymentIntent intent = PaymentIntent.create(params, options);
            log.info("Stripe Connect (direct charge): PaymentIntent {} créé sur le compte {} (commission {} % = {} c)",
                    intent.getId(), connectedAccountId, platformFeePercent, platformFee);

            payment.setStripePaymentIntentId(intent.getId());
            payment.setPaymentMethod("stripe");
            paymentRepository.save(payment);

            return new StripePaymentIntentResponse(
                    intent.getClientSecret(),
                    intent.getId(),
                    intent.getStatus()
            );
        } catch (StripeException e) {
            log.error("Failed to create Stripe PaymentIntent: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create payment intent: " + e.getMessage());
        }
    }

    /**
     * Crée une session Stripe Checkout (hébergée) pour régler un loyer.
     * Le locataire (payeur) paie par carte sur la page Stripe ; en direct charge, l'argent est
     * encaissé directement sur le compte Connect du propriétaire, moins la commission
     * plateforme — jamais de reversement à faire depuis le compte de la plateforme.
     */
    @Transactional
    public CheckoutSessionResponse createRentCheckoutSession(UUID paymentId) {
        Payment payment = findPaymentAndCheckAccess(paymentId);

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Ce paiement est déjà réglé.");
        }

        User owner = payment.getRental().getProperty().getOwner();
        String connectedAccountId = requireOnlinePaymentEnabled(owner);

        try {
            long amountInCents = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
            BigDecimal feePercent = platformFeeService.getFeePercentPourProprietaire(owner.getId());
            long platformFee = Math.round(amountInCents * feePercent.doubleValue() / 100.0);

            SessionCreateParams.PaymentIntentData piData = SessionCreateParams.PaymentIntentData.builder()
                    .putMetadata("payment_id", payment.getId().toString())
                    .putMetadata("rental_id", payment.getRental().getId().toString())
                    .setApplicationFeeAmount(platformFee)
                    .build();
            log.info("Rent checkout (direct charge): compte {} (commission {} c)", connectedAccountId, platformFee);

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/payments/" + paymentId + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/payments/" + paymentId)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(payment.getCurrency().toLowerCase())
                                    .setUnitAmount(amountInCents)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Loyer — " + payment.getRental().getProperty().getName())
                                            .build())
                                    .build())
                            .build())
                    .setPaymentIntentData(piData)
                    .putMetadata("payment_id", payment.getId().toString())
                    .build();
            RequestOptions options = RequestOptions.builder().setStripeAccount(connectedAccountId).build();

            Session session = Session.create(params, options);
            payment.setPaymentMethod("stripe");
            paymentRepository.save(payment);

            log.info("Rent checkout session {} créée pour paiement {}", session.getId(), paymentId);
            return new CheckoutSessionResponse(session.getId(), session.getUrl());
        } catch (StripeException e) {
            log.error("Échec création session checkout loyer: {}", e.getMessage(), e);
            throw new BadRequestException("Échec de la création du paiement Stripe : " + e.getMessage());
        }
    }

    /**
     * Vérifie que le propriétaire a explicitement activé le paiement carte en ligne et que son
     * compte Connect est opérationnel ; renvoie l'id du compte Connect à utiliser sinon lève.
     */
    private String requireOnlinePaymentEnabled(User owner) {
        String connectedAccountId = owner.getStripeConnectAccountId();
        boolean enabled = Boolean.TRUE.equals(owner.getAcceptOnlinePayments())
                && "COMPLETED".equals(owner.getStripeConnectStatus())
                && connectedAccountId != null && !connectedAccountId.isBlank();
        if (!enabled) {
            throw new BadRequestException(
                    "Le paiement en ligne n'est pas activé pour ce propriétaire. Réglez ce loyer directement avec lui.");
        }
        return connectedAccountId;
    }

    /**
     * Finalise un paiement de loyer au retour de Stripe Checkout : vérifie
     * auprès de Stripe que la session est payée, puis passe le paiement en
     * PAID et génère la quittance. Permet de fonctionner sans webhook en local.
     */
    @Transactional
    public PaymentResponse finalizeCheckout(UUID paymentId, String sessionId) {
        Payment payment = findPaymentAndCheckAccess(paymentId);

        if (payment.getStatus() == PaymentStatus.PAID) {
            return PaymentResponse.from(payment);
        }

        try {
            // Session créée en direct charge sur le compte Connect du propriétaire : elle doit
            // être récupérée avec le même compte, sinon Stripe renvoie une 404.
            String connectedAccountId = payment.getRental().getProperty().getOwner().getStripeConnectAccountId();
            Session session = connectedAccountId != null && !connectedAccountId.isBlank()
                    ? Session.retrieve(sessionId, RequestOptions.builder().setStripeAccount(connectedAccountId).build())
                    : Session.retrieve(sessionId);
            if (!"paid".equals(session.getPaymentStatus())) {
                throw new BadRequestException("Le paiement n'a pas encore été confirmé par Stripe.");
            }
            // La session doit correspondre AU paiement ciblé : sans ce lien, n'importe quelle
            // session « paid » à laquelle l'appelant a accès validerait ce paiement (rejeu / paiement
            // d'un loyer réglé par la session d'un autre). Le metadata payment_id est posé à la création.
            String sessionPaymentId = session.getMetadata() != null ? session.getMetadata().get("payment_id") : null;
            if (!paymentId.toString().equals(sessionPaymentId)) {
                log.warn("Session Stripe {} non liée au paiement {} (metadata payment_id={})",
                        sessionId, paymentId, sessionPaymentId);
                throw new BadRequestException("Cette session de paiement ne correspond pas à ce règlement.");
            }

            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            if (session.getPaymentIntent() != null) {
                payment.setStripePaymentIntentId(session.getPaymentIntent());
            }
            payment.setPaymentMethod("stripe");
            paymentRepository.save(payment);

            try {
                receiptService.generateReceipt(paymentId);
            } catch (Exception e) {
                log.error("Échec génération quittance paiement {}: {}", paymentId, e.getMessage());
            }
            auditService.logAction(AuditAction.PAYMENT_CONFIRMED, "Payment", paymentId);

            log.info("Paiement {} réglé via Stripe Checkout (session {})", paymentId, sessionId);
            return PaymentResponse.from(payment);
        } catch (StripeException e) {
            log.error("Échec vérification session Stripe {}: {}", sessionId, e.getMessage(), e);
            throw new BadRequestException("Impossible de vérifier la session Stripe : " + e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse confirmPayment(UUID paymentId) {
        Payment payment = findPaymentAndCheckAccess(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.LATE) {
            throw new BadRequestException("Payment cannot be confirmed in status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("Payment {} confirmed manually", paymentId);

        // Generate receipt (quittance) automatically
        try {
            receiptService.generateReceipt(paymentId);
        } catch (Exception e) {
            log.error("Failed to generate receipt for payment {}: {}", paymentId, e.getMessage());
        }
        auditService.logAction(AuditAction.PAYMENT_CONFIRMED, "Payment", paymentId);

        return PaymentResponse.from(payment);
    }

    @Transactional
    public void processStripeWebhook(String payload, String sigHeader) {
        handleStripeEvent(verifyAndParse(payload, sigHeader, stripeWebhookSecret));
    }

    /**
     * Webhook de l'endpoint « Connected accounts » : les paiements de loyer sont des direct
     * charges sur le compte Connect du bailleur, leurs événements n'arrivent QUE par ici
     * (l'endpoint plateforme ne les voit jamais). Secret de signature distinct.
     */
    public void processStripeConnectWebhook(String payload, String sigHeader) {
        handleStripeEvent(verifyAndParse(payload, sigHeader, stripeConnectWebhookSecret));
    }

    private Event verifyAndParse(String payload, String sigHeader, String secret) {
        try {
            if (secret != null && !secret.isBlank()) {
                return Webhook.constructEvent(payload, sigHeader, secret);
            }
            // Secret absent avec Stripe configuré : on REFUSE — accepter un événement non signé
            // permettrait de forger un payment_intent.succeeded et de solder un loyer.
            if (stripeApiKey != null && !stripeApiKey.isBlank()) {
                log.error("Webhook Stripe reçu sans secret configuré : événement rejeté");
                throw new BadRequestException("Webhook secret not configured");
            }
            // Ni clé ni secret = dev/local sans Stripe.
            return ApiResource.GSON.fromJson(payload, Event.class);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            // Inclut l'en-tête Stripe-Signature absent (NPE de la lib) : même verdict.
            log.error("Stripe webhook signature verification failed", e);
            throw new BadRequestException("Invalid Stripe signature");
        }
    }

    /** Dispatch d'un événement Stripe déjà vérifié (appelé par les deux endpoints). */
    public void handleStripeEvent(Event event) {
        log.info("Processing Stripe event: {}", event.getType());
        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
            // account.updated est traité en amont par WebhookController (statut Connect) : rien à
            // faire ici, on l'ignore silencieusement pour ne pas polluer les logs.
            case "account.updated" -> { }
            default -> log.info("Unhandled Stripe event type: {}", event.getType());
        }
    }

    /**
     * Lit l'objet de l'événement en JSON BRUT : getObject() renvoie vide dès que la version
     * d'API de l'événement diffère de celle du SDK (même piège que les abonnements) — le JSON
     * brut est la source fiable.
     */
    private com.fasterxml.jackson.databind.JsonNode readEventObject(Event event) {
        String rawJson = event.getDataObjectDeserializer().getRawJson();
        if (rawJson == null || rawJson.isBlank()) {
            log.warn("Événement Stripe {} sans données JSON exploitables", event.getType());
            return null;
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception e) {
            log.error("JSON d'événement Stripe illisible ({})", event.getType(), e);
            return null;
        }
    }

    /**
     * Retrouve le paiement visé par un PaymentIntent. Le flux Checkout ne stocke pas l'id de
     * l'intent avant le paiement (il naît chez Stripe) : la metadata payment_id — posée à la
     * création de la session — est la référence primaire ; l'id d'intent sert de repli (flux
     * historique PaymentIntent direct).
     */
    private java.util.Optional<Payment> resolveWebhookPayment(com.fasterxml.jackson.databind.JsonNode intent) {
        String metaPaymentId = intent.path("metadata").path("payment_id").asText(null);
        if (metaPaymentId != null && !metaPaymentId.isBlank()) {
            try {
                java.util.Optional<Payment> byId = paymentRepository.findById(UUID.fromString(metaPaymentId));
                if (byId.isPresent()) return byId;
            } catch (IllegalArgumentException ignored) {
                // metadata étrangère (pas un UUID) : on retombe sur l'id d'intent.
            }
        }
        String intentId = intent.path("id").asText(null);
        return intentId != null ? paymentRepository.findByStripePaymentIntentId(intentId) : java.util.Optional.empty();
    }

    private void handlePaymentIntentSucceeded(Event event) {
        com.fasterxml.jackson.databind.JsonNode intent = readEventObject(event);
        if (intent == null) return;

        resolveWebhookPayment(intent).ifPresent(payment -> {
            // Idempotent : le retour navigateur (finalizeCheckout) a pu finaliser avant nous.
            if (payment.getStatus() == PaymentStatus.PAID) {
                // Compléter la traçabilité si le retour navigateur n'avait pas la charge.
                if (payment.getStripeChargeId() == null) {
                    payment.setStripeChargeId(intent.path("latest_charge").asText(null));
                    paymentRepository.save(payment);
                }
                return;
            }

            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setStripePaymentIntentId(intent.path("id").asText(null));
            payment.setStripeChargeId(intent.path("latest_charge").asText(null));
            payment.setPaymentMethod("stripe");
            paymentRepository.save(payment);

            log.info("Payment {} marked as PAID via Stripe webhook", payment.getId());

            try {
                receiptService.generateReceipt(payment.getId());
            } catch (Exception e) {
                log.error("Failed to generate receipt for payment {}: {}", payment.getId(), e.getMessage());
            }
            auditService.logAction(AuditAction.PAYMENT_CONFIRMED, "Payment", payment.getId());
        });
    }

    private void handlePaymentIntentFailed(Event event) {
        com.fasterxml.jackson.databind.JsonNode intent = readEventObject(event);
        if (intent == null) return;

        resolveWebhookPayment(intent).ifPresent(payment -> {
            // Un paiement déjà soldé ne repasse jamais FAILED (événement retardataire/rejoué).
            if (payment.getStatus() == PaymentStatus.PAID) return;
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Payment {} marked as FAILED via Stripe webhook", payment.getId());
        });
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void markOverduePayments() {
        List<Payment> overduePayments = paymentRepository.findOverduePayments(LocalDate.now());
        for (Payment payment : overduePayments) {
            payment.setStatus(PaymentStatus.LATE);
            paymentRepository.save(payment);
            log.info("Payment {} marked as LATE (due date: {})", payment.getId(), payment.getDueDate());

            // Notifier le propriétaire du retard de paiement
            try {
                UUID ownerId = payment.getRental().getProperty().getOwner().getId();
                String tenantName = payment.getPayer().getFullName();
                notificationService.create(
                        ownerId,
                        NotificationType.PAYMENT_OVERDUE,
                        "Paiement en retard",
                        "Le paiement de " + payment.getAmount() + " " + payment.getCurrency()
                                + " de " + tenantName + " est en retard.",
                        "/payments"
                );
            } catch (Exception e) {
                log.error("Échec de la création de notification pour le paiement en retard {}", payment.getId(), e);
            }
        }
        if (!overduePayments.isEmpty()) {
            log.info("Marked {} payments as LATE", overduePayments.size());
        }
    }

    /**
     * Envoie des rappels de paiement automatiques :
     * - J-3 : rappel amical
     * - J-1 : rappel urgent
     * - J+1 (LATE) : avis de retard
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional(readOnly = true)
    public void sendPaymentReminders() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // J-3 : rappel amical pour les paiements dus dans 3 jours
        List<Payment> dueIn3Days = paymentRepository.findPaymentsDueOn(today.plusDays(3));
        log.info("Found {} payments due in 3 days (J-3)", dueIn3Days.size());
        for (Payment payment : dueIn3Days) {
            try {
                sendReminderForPayment(payment, false, dateFormatter);
            } catch (Exception e) {
                log.error("Failed to send J-3 reminder for payment {}: {}", payment.getId(), e.getMessage(), e);
            }
        }

        // J-1 : rappel urgent pour les paiements dus demain
        List<Payment> dueTomorrow = paymentRepository.findPaymentsDueOn(today.plusDays(1));
        log.info("Found {} payments due tomorrow (J-1)", dueTomorrow.size());
        for (Payment payment : dueTomorrow) {
            try {
                sendReminderForPayment(payment, false, dateFormatter);
            } catch (Exception e) {
                log.error("Failed to send J-1 reminder for payment {}: {}", payment.getId(), e.getMessage(), e);
            }
        }

        // J+1 : avis de retard pour les paiements marqués LATE hier
        List<Payment> lateYesterday = paymentRepository.findLatePaymentsDueOn(today.minusDays(1));
        log.info("Found {} late payments from yesterday (J+1)", lateYesterday.size());
        for (Payment payment : lateYesterday) {
            try {
                sendReminderForPayment(payment, true, dateFormatter);
            } catch (Exception e) {
                log.error("Failed to send overdue notice for payment {}: {}", payment.getId(), e.getMessage(), e);
            }
        }
    }

    private void sendReminderForPayment(Payment payment, boolean isOverdue, DateTimeFormatter dateFormatter) {
        Rental rental = payment.getRental();
        Property property = rental.getProperty();
        User tenant = payment.getPayer();
        User owner = property.getOwner();

        String propertyAddress = property.getAddressLine1() != null
                ? property.getAddressLine1() + ", " + property.getPostalCode() + " " + property.getCity()
                : property.getCity() != null ? property.getCity() : "";

        String paymentUrl = frontendUrl + "/payments/" + payment.getId();

        PaymentReminderEmailData emailData = PaymentReminderEmailData.builder()
                .recipientEmail(tenant.getEmail())
                .recipientName(tenant.getFirstName() + " " + tenant.getLastName())
                .ownerName(owner.getFirstName() + " " + owner.getLastName())
                .propertyName(property.getName())
                .propertyAddress(propertyAddress)
                .amount(payment.getAmount().toPlainString())
                .currency(payment.getCurrency())
                .dueDate(payment.getDueDate().format(dateFormatter))
                .paymentUrl(paymentUrl)
                .isOverdue(isOverdue)
                .build();

        emailService.sendPaymentReminderEmail(emailData);
        log.info("Payment reminder sent to {} for payment {} (overdue: {})",
                tenant.getEmail(), payment.getId(), isOverdue);
    }

    private Payment findPaymentAndCheckAccess(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId.toString()));

        // Le payeur du paiement y accède ; sinon il faut être propriétaire du bien (ou admin).
        if (!payment.getPayer().getId().equals(AuthService.getCurrentUserId())) {
            rentalAccessService.assertOwner(payment.getRental(), "You are not authorized to access this payment");
        }

        return payment;
    }
}
