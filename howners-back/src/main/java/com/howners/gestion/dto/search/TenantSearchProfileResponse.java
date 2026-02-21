package com.howners.gestion.dto.search;

import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.domain.search.FurnishedPreference;
import com.howners.gestion.domain.search.TenantSearchProfile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TenantSearchProfileResponse(
        UUID id,
        UUID tenantId,
        String tenantName,
        String tenantEmail,
        String desiredCity,
        String desiredDepartment,
        String desiredPostalCode,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        PropertyType desiredPropertyType,
        BigDecimal minSurface,
        Integer minBedrooms,
        FurnishedPreference furnishedPreference,
        LocalDate desiredMoveIn,
        String description,
        Boolean isActive,
        LocalDateTime updatedAt
) {
    public static TenantSearchProfileResponse from(TenantSearchProfile p) {
        return new TenantSearchProfileResponse(
                p.getId(),
                p.getTenant().getId(),
                p.getTenant().getFullName(),
                p.getTenant().getEmail(),
                p.getDesiredCity(),
                p.getDesiredDepartment(),
                p.getDesiredPostalCode(),
                p.getBudgetMin(),
                p.getBudgetMax(),
                p.getDesiredPropertyType(),
                p.getMinSurface(),
                p.getMinBedrooms(),
                p.getFurnishedPreference(),
                p.getDesiredMoveIn(),
                p.getDescription(),
                p.getIsActive(),
                p.getUpdatedAt()
        );
    }
}
