package com.howners.gestion.dto.template;

import com.howners.gestion.domain.contract.ContractTemplate;
import com.howners.gestion.domain.rental.RentalType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ContractTemplateResponse(
        UUID id,
        String name,
        String description,
        RentalType rentalType,
        String content,
        Boolean isDefault,
        Boolean isActive,
        UUID createdById,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ContractTemplateResponse from(ContractTemplate template) {
        return new ContractTemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getRentalType(),
                template.getContent(),
                template.getIsDefault(),
                template.getIsActive(),
                template.getCreatedBy() != null ? template.getCreatedBy().getId() : null,
                template.getCreatedBy() != null ? getFullName(template.getCreatedBy()) : null,
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    private static String getFullName(com.howners.gestion.domain.user.User user) {
        if (user == null) return "";
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }
}
