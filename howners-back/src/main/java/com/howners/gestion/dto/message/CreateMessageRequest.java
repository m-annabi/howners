package com.howners.gestion.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateMessageRequest(
        @NotNull UUID recipientId,
        String subject,
        @NotBlank String body,
        UUID listingId,
        UUID applicationId,
        UUID parentId
) {
}
