package com.howners.gestion.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExitTenantRequest(
        @NotNull LocalDate exitDate,
        String notes
) {}
