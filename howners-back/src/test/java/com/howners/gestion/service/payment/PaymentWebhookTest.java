package com.howners.gestion.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.payment.PaymentType;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.audit.AuditService;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.notification.NotificationDispatcher;
import com.howners.gestion.service.notification.NotificationService;
import com.howners.gestion.service.receipt.ReceiptService;
import com.howners.gestion.service.rental.RentalAccessService;
import com.stripe.model.Event;
import com.stripe.net.ApiResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Webhook Stripe des paiements de loyer : résolution par metadata payment_id (le flux Checkout
 * ne connaît pas l'id d'intent avant paiement), lecture en JSON brut, idempotence.
 */
@ExtendWith(MockitoExtension.class)
class PaymentWebhookTest {

    @Mock PaymentRepository paymentRepository;
    @Mock RentalRepository rentalRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock ReceiptService receiptService;
    @Mock EmailService emailService;
    @Mock NotificationService notificationService;
    @Mock PlatformFeeService platformFeeService;
    @Mock RentalAccessService rentalAccessService;
    @Mock NotificationDispatcher notificationDispatcher;
    @Spy ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks PaymentService paymentService;

    UUID paymentId;
    Payment payment;

    @BeforeEach
    void setup() {
        paymentId = UUID.randomUUID();
        payment = Payment.builder()
                .id(paymentId)
                .paymentType(PaymentType.RENT)
                .amount(new BigDecimal("850.00"))
                .currency("EUR")
                .status(PaymentStatus.PENDING)
                .build();
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Event intentEvent(String type, String metadataPaymentId) {
        String meta = metadataPaymentId != null
                ? "\"metadata\":{\"payment_id\":\"" + metadataPaymentId + "\"},"
                : "\"metadata\":{},";
        String json = "{\"type\":\"" + type + "\",\"data\":{\"object\":{"
                + "\"id\":\"pi_test_123\",\"object\":\"payment_intent\","
                + meta
                + "\"latest_charge\":\"ch_test_456\"}}}";
        return ApiResource.GSON.fromJson(json, Event.class);
    }

    @Test
    void succeeded_resolutParMetadataPaymentId_etSoldeAvecQuittance() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        paymentService.handleStripeEvent(intentEvent("payment_intent.succeeded", paymentId.toString()));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.getStripePaymentIntentId()).isEqualTo("pi_test_123");
        assertThat(payment.getStripeChargeId()).isEqualTo("ch_test_456");
        verify(receiptService).generateReceipt(paymentId);
    }

    @Test
    void succeeded_sansMetadata_retombeSurLIdDIntent() {
        when(paymentRepository.findByStripePaymentIntentId("pi_test_123")).thenReturn(Optional.of(payment));

        paymentService.handleStripeEvent(intentEvent("payment_intent.succeeded", null));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(receiptService).generateReceipt(paymentId);
    }

    @Test
    void succeeded_estIdempotent_siDejaSoldeParLeRetourNavigateur() {
        payment.setStatus(PaymentStatus.PAID);
        payment.setStripeChargeId("ch_deja_la");
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        paymentService.handleStripeEvent(intentEvent("payment_intent.succeeded", paymentId.toString()));

        // Ni re-sauvegarde ni double quittance.
        verify(paymentRepository, never()).save(any());
        verify(receiptService, never()).generateReceipt(any());
    }

    @Test
    void failed_marqueEchoue_maisJamaisUnPaiementDejaSolde() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        paymentService.handleStripeEvent(intentEvent("payment_intent.payment_failed", paymentId.toString()));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);

        // Événement retardataire après finalisation : le statut PAID est conservé.
        payment.setStatus(PaymentStatus.PAID);
        paymentService.handleStripeEvent(intentEvent("payment_intent.payment_failed", paymentId.toString()));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }
}
