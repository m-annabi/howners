package com.howners.gestion.dto.payment;

import com.howners.gestion.domain.payment.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID rentalId,
        @NotNull PaymentType paymentType,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String currency,
        LocalDate dueDate,
        String paymentMethod
) {}
