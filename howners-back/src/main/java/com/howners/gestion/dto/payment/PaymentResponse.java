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
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment p) {
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
                p.getCreatedAt()
        );
    }
}
