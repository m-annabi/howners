package com.howners.gestion.dto.audit;

import com.howners.gestion.domain.audit.ConsentType;
import com.howners.gestion.domain.audit.UserConsent;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsentResponse(
        UUID id,
        ConsentType consentType,
        Boolean granted,
        LocalDateTime grantedAt,
        LocalDateTime revokedAt,
        LocalDateTime updatedAt
) {
    public static ConsentResponse from(UserConsent consent) {
        return new ConsentResponse(
                consent.getId(),
                consent.getConsentType(),
                consent.getGranted(),
                consent.getGrantedAt(),
                consent.getRevokedAt(),
                consent.getUpdatedAt()
        );
    }
}
