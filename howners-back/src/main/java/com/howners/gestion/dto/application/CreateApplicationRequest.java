package com.howners.gestion.dto.application;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateApplicationRequest(
        @NotNull UUID listingId,
        String coverLetter,
        LocalDate desiredMoveIn
) {
}
