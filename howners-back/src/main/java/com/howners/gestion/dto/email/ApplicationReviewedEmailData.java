package com.howners.gestion.dto.email;

import lombok.Builder;

@Builder
public record ApplicationReviewedEmailData(
        String recipientEmail,
        String recipientName,
        String ownerName,
        String propertyName,
        String listingTitle,
        String status,
        String notes,
        String dashboardUrl
) {}
