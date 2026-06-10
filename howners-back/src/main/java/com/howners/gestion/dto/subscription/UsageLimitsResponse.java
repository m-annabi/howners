package com.howners.gestion.dto.subscription;

public record UsageLimitsResponse(
        String planName,
        int currentProperties,
        int maxProperties,
        int currentContractsThisMonth,
        int maxContractsPerMonth,
        boolean canCreateProperty,
        boolean canCreateContract,
        boolean canCreateRental,
        boolean canCreateListing,
        boolean hasESignature,
        boolean hasTenantScoring,
        boolean hasDocumentEncryption
) {}
