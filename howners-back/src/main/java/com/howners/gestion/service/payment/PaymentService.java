package com.howners.gestion.service.payment;

import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
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

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

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

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(payment.getCurrency().toLowerCase())
                    .putMetadata("payment_id", payment.getId().toString())
                    .putMetadata("rental_id", payment.getRental().getId().toString())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

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
