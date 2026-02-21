package com.howners.gestion.dto.search;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateInvitationRequest(
        @NotNull UUID listingId,
        @NotNull UUID tenantId,
        String message
) {
}
