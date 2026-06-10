package com.howners.gestion.service.payment;

import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
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
import com.howners.gestion.service.receipt.ReceiptService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.ApiResource;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
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

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @Value("${stripe.platform-fee-percent:2.5}")
    private double platformFeePercent;

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
        if (!rental.getProperty().getOwner().getId().equals(currentUserId) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You are not authorized to create payments for this rental");
        }

        User payer = rental.getTenant();
        if (payer == null) {
            throw new BadRequestException("Cannot create payment: no tenant assigned to this rental");
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

        return PaymentResponse.from(payment);
    }

    @Transactional
    public StripePaymentIntentResponse createStripePaymentIntent(UUID paymentId) {
        Payment payment = findPaymentAndCheckAccess(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Payment is not in PENDING status");
        }

        try {
            long amountInCents = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
            long platformFee = Math.round(amountInCents * platformFeePercent / 100.0);

            User owner = payment.getRental().getProperty().getOwner();
            String connectedAccountId = owner.getStripeConnectAccountId();

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(payment.getCurrency().toLowerCase())
                    .putMetadata("payment_id", payment.getId().toString())
                    .putMetadata("rental_id", payment.getRental().getId().toString());

            if (connectedAccountId != null && !connectedAccountId.isBlank()) {
                paramsBuilder
                        .setApplicationFeeAmount(platformFee)
                        .setTransferData(PaymentIntentCreateParams.TransferData.builder()
                                .setDestination(connectedAccountId)
                                .build());
                log.info("Stripe Connect: routing payment to {} with {}% platform fee ({} cents)",
                        connectedAccountId, platformFeePercent, platformFee);
            } else {
                log.info("Owner {} has no Stripe Connect account — payment without transfer", owner.getId());
            }

            PaymentIntent intent = PaymentIntent.create(paramsBuilder.build());

            payment.setStripePaymentIntentId(intent.getId());
            payment.setPaymentMethod("stripe");
            paymentRepository.save(payment);

            log.info("Stripe PaymentIntent created: {} for payment {}", intent.getId(), paymentId);

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
