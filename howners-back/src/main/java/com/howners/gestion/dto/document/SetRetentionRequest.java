package com.howners.gestion.dto.document;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SetRetentionRequest(
        @NotNull LocalDate retentionEndDate
) {}
