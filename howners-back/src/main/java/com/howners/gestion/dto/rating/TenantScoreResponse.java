package com.howners.gestion.dto.rating;

import java.util.UUID;

public record TenantScoreResponse(
        UUID tenantId,
        String tenantName,
        int score,
        RiskLevel riskLevel,
        ScoreBreakdown breakdown,
        PaymentStats paymentStats
) {

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }

    public record ScoreBreakdown(
            double paymentScore,
            double ratingScore,
            double leaseDurationScore,
            double communicationScore
    ) {}

    public record PaymentStats(
            long totalPayments,
            long onTimePayments,
            long latePayments,
            double onTimePercentage
    ) {}
}
