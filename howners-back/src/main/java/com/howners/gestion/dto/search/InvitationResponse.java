package com.howners.gestion.dto.search;

import com.howners.gestion.domain.search.InvitationStatus;
import com.howners.gestion.domain.search.TenantInvitation;

import java.time.LocalDateTime;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID listingId,
        String listingTitle,
        UUID tenantId,
        String tenantName,
        UUID ownerId,
        String ownerName,
        String message,
        InvitationStatus status,
        LocalDateTime createdAt
) {
    public static InvitationResponse from(TenantInvitation inv) {
        return new InvitationResponse(
                inv.getId(),
                inv.getListing().getId(),
                inv.getListing().getTitle(),
                inv.getTenant().getId(),
                inv.getTenant().getFullName(),
                inv.getOwner().getId(),
                inv.getOwner().getFullName(),
                inv.getMessage(),
                inv.getStatus(),
                inv.getCreatedAt()
        );
    }
}
