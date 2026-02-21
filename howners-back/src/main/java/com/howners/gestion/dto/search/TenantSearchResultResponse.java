package com.howners.gestion.dto.search;

import com.howners.gestion.dto.rating.TenantScoreResponse;

public record TenantSearchResultResponse(
        TenantSearchProfileResponse profile,
        TenantScoreResponse tenantScore,
        Integer compatibilityScore,
        CompatibilityBreakdown compatibility
) {
    public record CompatibilityBreakdown(
            int zoneScore,
            int budgetScore,
            int propertyTypeScore,
            int surfaceScore,
            int bedroomScore,
            int furnishedScore
    ) {}
}
