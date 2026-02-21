package com.howners.gestion.dto.contract;

import com.howners.gestion.domain.contract.Contract;
import com.howners.gestion.domain.contract.ContractStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ContractResponse(
        UUID id,
        String contractNumber,
        UUID rentalId,
        String rentalPropertyName,
        String tenantFullName,
        ContractStatus status,
        Integer currentVersion,
        String documentUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime sentAt,
        LocalDateTime signedAt
) {
    public static ContractResponse from(Contract contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getContractNumber(),
                contract.getRental().getId(),
                contract.getRental().getProperty().getName(),
                getFullName(contract.getRental().getTenant()),
                contract.getStatus(),
                contract.getCurrentVersion(),
                contract.getDocumentUrl(),
                contract.getCreatedAt(),
                contract.getUpdatedAt(),
                contract.getSentAt(),
                contract.getSignedAt()
        );
    }

    private static String getFullName(com.howners.gestion.domain.user.User user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }
}
