package com.howners.gestion.dto.payment;

import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.payment.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID rentalId,
        String propertyName,
        UUID payerId,
        String payerName,
        PaymentType paymentType,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String paymentMethod,
        String stripePaymentIntentId,
        String receiptUrl,
        LocalDate dueDate,
        LocalDateTime paidAt,
        Integer relanceNiveau,
        LocalDateTime derniereRelanceLe,
        String miseEnDemeureNumero,
        LocalDateTime createdAt,
        String paymentInstructions,
        boolean onlinePaymentAvailable,
        /** Règlement hors plateforme déclaré par le locataire, en attente de confirmation. */
        LocalDateTime declaredAt,
        String declaredMethod,
        /** Commission plateforme (%) prélevée sur un paiement carte — renseignée sur le détail seulement. */
        BigDecimal platformFeePercent
) {
    public static PaymentResponse from(Payment p) {
        return from(p, null);
    }

    public static PaymentResponse from(Payment p, BigDecimal platformFeePercent) {
        var owner = p.getRental().getProperty().getOwner();
        boolean onlinePaymentAvailable = Boolean.TRUE.equals(owner.getAcceptOnlinePayments())
                && "COMPLETED".equals(owner.getStripeConnectStatus());
        return new PaymentResponse(
                p.getId(),
                p.getRental().getId(),
                p.getRental().getProperty().getName(),
                p.getPayer().getId(),
                p.getPayer().getFullName(),
                p.getPaymentType(),
                p.getAmount(),
                p.getCurrency(),
                p.getStatus(),
                p.getPaymentMethod(),
                p.getStripePaymentIntentId(),
                p.getReceiptUrl(),
                p.getDueDate(),
                p.getPaidAt(),
                p.getRelanceNiveau(),
                p.getDerniereRelanceLe(),
                p.getMiseEnDemeureNumero(),
                p.getCreatedAt(),
                owner.getPaymentInstructions(),
                onlinePaymentAvailable,
                p.getDeclaredAt(),
                p.getDeclaredMethod(),
                platformFeePercent
        );
    }
}
