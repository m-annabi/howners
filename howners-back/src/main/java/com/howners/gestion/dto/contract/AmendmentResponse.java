package com.howners.gestion.dto.contract;

import com.howners.gestion.domain.contract.AmendmentStatus;
import com.howners.gestion.domain.contract.ContractAmendment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AmendmentResponse(
        UUID id,
        UUID contractId,
        String contractNumber,
        Integer amendmentNumber,
        String reason,
        String changes,
        BigDecimal previousRent,
        BigDecimal newRent,
        LocalDate effectiveDate,
        AmendmentStatus status,
        String createdByName,
        LocalDateTime signedAt,
        UUID documentId,
        LocalDateTime createdAt
) {
    public static AmendmentResponse from(ContractAmendment a) {
        return new AmendmentResponse(
                a.getId(),
                a.getContract().getId(),
                a.getContract().getContractNumber(),
                a.getAmendmentNumber(),
                a.getReason(),
                a.getChanges(),
                a.getPreviousRent(),
                a.getNewRent(),
                a.getEffectiveDate(),
                a.getStatus(),
                a.getCreatedBy().getFullName(),
                a.getSignedAt(),
                a.getDocument() != null ? a.getDocument().getId() : null,
                a.getCreatedAt()
        );
    }
}
