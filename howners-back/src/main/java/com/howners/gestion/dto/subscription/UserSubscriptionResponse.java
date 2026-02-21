package com.howners.gestion.dto.subscription;

import com.howners.gestion.domain.subscription.SubscriptionStatus;
import com.howners.gestion.domain.subscription.UserSubscription;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserSubscriptionResponse(
        UUID id,
        UUID userId,
        SubscriptionPlanResponse plan,
        SubscriptionStatus status,
        LocalDateTime currentPeriodStart,
        LocalDateTime currentPeriodEnd,
        Boolean cancelAtPeriodEnd,
        LocalDateTime createdAt
) {
    public static UserSubscriptionResponse from(UserSubscription sub) {
        return new UserSubscriptionResponse(
                sub.getId(),
                sub.getUser().getId(),
                SubscriptionPlanResponse.from(sub.getPlan()),
                sub.getStatus(),
                sub.getCurrentPeriodStart(),
                sub.getCurrentPeriodEnd(),
                sub.getCancelAtPeriodEnd(),
                sub.getCreatedAt()
        );
    }
}
