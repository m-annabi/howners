package com.howners.gestion.dto.search;

import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.domain.search.FurnishedPreference;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTenantSearchProfileRequest(
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
        String description
) {
}
