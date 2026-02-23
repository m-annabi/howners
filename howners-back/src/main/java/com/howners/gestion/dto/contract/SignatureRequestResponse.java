package com.howners.gestion.dto.contract;

import com.howners.gestion.domain.contract.SignatureRequestStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Réponse contenant les informations d'une demande de signature
 */
@Builder
public record SignatureRequestResponse(
        UUID id,
        UUID contractId,
        String contractNumber,
        String provider,
        String signerEmail,
        String signerName,
        SignatureRequestStatus status,
        LocalDateTime sentAt,
        LocalDateTime viewedAt,
        LocalDateTime signedAt,
        LocalDateTime declinedAt,
        LocalDateTime tokenExpiresAt,
        Integer resendCount,
        String declineReason,
        Integer reminderCount,
        LocalDateTime lastReminderAt,
        Integer signerOrder
) {}
