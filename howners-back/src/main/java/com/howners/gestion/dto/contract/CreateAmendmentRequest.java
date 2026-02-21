package com.howners.gestion.dto.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateAmendmentRequest(
        @NotBlank String reason,
        String changes,
        BigDecimal previousRent,
        BigDecimal newRent,
        @NotNull LocalDate effectiveDate
) {
}
