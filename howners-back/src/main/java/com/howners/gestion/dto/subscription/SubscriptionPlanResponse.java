package com.howners.gestion.dto.subscription;

import com.howners.gestion.domain.subscription.PlanName;
import com.howners.gestion.domain.subscription.SubscriptionPlan;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record SubscriptionPlanResponse(
        UUID id,
        PlanName name,
        String displayName,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        Integer maxProperties,
        Integer maxContractsPerMonth,
        Map<String, Object> features
) {
    public static SubscriptionPlanResponse from(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDisplayName(),
                plan.getMonthlyPrice(),
                plan.getAnnualPrice(),
                plan.getMaxProperties(),
                plan.getMaxContractsPerMonth(),
                plan.getFeatures()
        );
    }
}
