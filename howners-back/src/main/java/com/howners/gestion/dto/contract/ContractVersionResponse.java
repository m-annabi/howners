package com.howners.gestion.dto.contract;

import com.howners.gestion.domain.contract.ContractVersion;

import java.time.LocalDateTime;
import java.util.UUID;

public record ContractVersionResponse(
        UUID id,
        UUID contractId,
        Integer version,
        String content,
        String documentUrl,
        String documentHash,
        LocalDateTime createdAt,
        UUID createdById,
        String createdByName
) {
    public static ContractVersionResponse from(ContractVersion version) {
        return from(version, version.getDocumentUrl());
    }

    public static ContractVersionResponse from(ContractVersion version, String presignedUrl) {
        return new ContractVersionResponse(
                version.getId(),
                version.getContract().getId(),
                version.getVersion(),
                version.getContent(),
                presignedUrl,
                version.getDocumentHash(),
                version.getCreatedAt(),
                version.getCreatedBy() != null ? version.getCreatedBy().getId() : null,
                version.getCreatedBy() != null ? getFullName(version.getCreatedBy()) : "System"
        );
    }

    private static String getFullName(com.howners.gestion.domain.user.User user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }
}
