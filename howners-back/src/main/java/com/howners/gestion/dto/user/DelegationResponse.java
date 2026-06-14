package com.howners.gestion.dto.user;

import com.howners.gestion.domain.user.CompteDelegation;
import com.howners.gestion.domain.user.StatutDelegation;

import java.time.LocalDateTime;
import java.util.UUID;

public record DelegationResponse(
        UUID id,
        UUID agenceUserId,
        String agenceNom,
        String agenceEmail,
        UUID delegueUserId,
        String delegueNom,
        String delegueEmail,
        StatutDelegation statut,
        LocalDateTime createdAt
) {
    public static DelegationResponse from(CompteDelegation delegation) {
        return new DelegationResponse(
                delegation.getId(),
                delegation.getAgence().getId(),
                delegation.getAgence().getFullName(),
                delegation.getAgence().getEmail(),
                delegation.getDelegue().getId(),
                delegation.getDelegue().getFullName(),
                delegation.getDelegue().getEmail(),
                delegation.getStatut(),
                delegation.getCreatedAt()
        );
    }
}
