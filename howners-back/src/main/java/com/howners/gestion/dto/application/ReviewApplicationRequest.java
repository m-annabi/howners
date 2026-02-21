package com.howners.gestion.dto.application;

import com.howners.gestion.domain.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewApplicationRequest(
        @NotNull ApplicationStatus status,
        String notes
) {
}
