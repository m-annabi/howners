package com.howners.gestion.dto.email;

import lombok.Builder;

@Builder
public record WeeklyDigestEmailData(
        String recipientEmail,
        String ownerName,
        long latePayments,
        long expiringContracts,
        long awaitingSignatures,
        long pendingApplications,
        String dashboardUrl
) {
    public boolean hasContent() {
        return latePayments + expiringContracts + awaitingSignatures + pendingApplications > 0;
    }
}
