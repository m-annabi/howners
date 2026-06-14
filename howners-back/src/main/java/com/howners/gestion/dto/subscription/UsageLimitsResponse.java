package com.howners.gestion.dto.subscription;

public record UsageLimitsResponse(
        String planName,
        int currentProperties,
        int maxProperties,
        int currentContractsThisMonth,
        int maxContractsPerMonth,
        int currentRentals,
        int maxRentals,
        int currentListings,
        int maxListings,
        boolean canCreateProperty,
        boolean canCreateContract,
        boolean canCreateRental,
        boolean canCreateListing,
        boolean hasESignature,
        boolean hasTenantScoring,
        boolean hasDocumentEncryption,
        boolean hasMultiAccount
) {}
