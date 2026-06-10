package com.howners.gestion.dto.application;

import com.howners.gestion.domain.rental.RentalType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateRentalFromApplicationRequest(
        @NotNull RentalType rentalType,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal monthlyRent,
        String currency,
        @DecimalMin("0.0") BigDecimal depositAmount,
        @DecimalMin("0.0") BigDecimal charges,
        @Min(1) @Max(31) Integer paymentDay
) {}
