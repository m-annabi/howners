package com.howners.gestion.service.payment;

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
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findByRentalId(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));
        assertRentalAccess(rental);
        return paymentRepository.findByRentalId(rentalId).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    /** Autorise le propriétaire du bien, le locataire du bail, ou un admin. */
    private void assertRentalAccess(Rental rental) {
        UUID currentUserId = AuthService.getCurrentUserId();
        UUID ownerId = rental.getProperty() != null && rental.getProperty().getOwner() != null
                ? rental.getProperty().getOwner().getId() : null;
        UUID tenantId = rental.getTenant() != null ? rental.getTenant().getId() : null;
        boolean isAdmin = userRepository.findById(currentUserId)
                .map(u -> u.getRole() == Role.ADMIN).orElse(false);
        if (!currentUserId.equals(ownerId) && !currentUserId.equals(tenantId) && !isAdmin) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à accéder à ce bail.");
        }
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        Rental rental = rentalRepository.findById(request.rentalId())
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", request.rentalId().toString()));

        // Only the property owner can create payments
        if (!rental.getProperty().getOwner().getId().equals(currentUserId) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You are not authorized to create payments for this rental");
        }

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

            notificationService.create(
                    tenant.getId(),
                    NotificationType.PAYMENT_DUE,
                    "Nouvelle échéance de paiement",
                    "Un paiement de " + amountLabel
                            + (dueDateLabel != null ? " à régler avant le " + dueDateLabel : " à régler")
                            + " a été enregistré par " + owner.getFullName() + ".",
                    "/payments/" + payment.getId());

            if (tenant.getEmail() != null) {
                String paymentUrl = frontendUrl + "/payments/" + payment.getId();
                String detailsHtml = "<strong>Bien :</strong> " + property.getName() + "<br>"
                        + "<strong>Montant :</strong> " + amountLabel
                        + (dueDateLabel != null ? "<br><strong>Échéance :</strong> " + dueDateLabel : "");

                emailService.sendNotificationEmail(new GenericNotificationEmailData(
                        tenant.getEmail(),
                        tenant.getFullName(),
                        "Nouvelle échéance de paiement — " + property.getName(),
                        "Nouvelle échéance de paiement",
                        "Votre propriétaire " + owner.getFullName() + " a enregistré une nouvelle échéance de paiement pour votre location.",
                        detailsHtml,
                        "Voir le paiement",
                        paymentUrl,
                        false
                ));
            }
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
        Event event;
        try {
            if (stripeWebhookSecret != null && !stripeWebhookSecret.isBlank()) {
                event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
            } else {
                event = ApiResource.GSON.fromJson(payload, Event.class);
            }
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed", e);
            throw new BadRequestException("Invalid Stripe signature");
        }

        log.info("Processing Stripe event: {}", event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
            default -> log.info("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handlePaymentIntentSucceeded(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (intent == null) return;

        paymentRepository.findByStripePaymentIntentId(intent.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setStripeChargeId(intent.getLatestCharge());
            paymentRepository.save(payment);

            log.info("Payment {} marked as PAID via Stripe webhook", payment.getId());

            try {
                receiptService.generateReceipt(payment.getId());
            } catch (Exception e) {
                log.error("Failed to generate receipt for payment {}: {}", payment.getId(), e.getMessage());
            }
        });
    }

    private void handlePaymentIntentFailed(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (intent == null) return;

        paymentRepository.findByStripePaymentIntentId(intent.getId()).ifPresent(payment -> {
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

        UUID currentUserId = AuthService.getCurrentUserId();
        UUID ownerId = payment.getRental().getProperty().getOwner().getId();
        UUID payerId = payment.getPayer().getId();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        if (!ownerId.equals(currentUserId) && !payerId.equals(currentUserId) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You are not authorized to access this payment");
        }

        return payment;
    }
}
