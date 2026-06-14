package com.howners.gestion.service.subscription;

import com.howners.gestion.domain.subscription.*;
import com.howners.gestion.dto.subscription.UsageLimitsResponse;
import com.howners.gestion.exception.PlanLimitExceededException;
import com.howners.gestion.repository.ListingRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UsageTrackingRepository;
import com.howners.gestion.repository.UserSubscriptionRepository;
import com.howners.gestion.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureGateService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UsageTrackingRepository usageTrackingRepository;
    private final PropertyRepository propertyRepository;
    private final RentalRepository rentalRepository;
    private final ListingRepository listingRepository;

    public boolean canCreateProperty(UUID userId) {
        SubscriptionPlan plan = getActivePlan(userId);
        if (plan.getMaxProperties() == -1) return true;

        int current = propertyRepository.findByOwnerId(userId).size();
        return current < plan.getMaxProperties();
    }

    public boolean canCreateContract(UUID userId) {
        SubscriptionPlan plan = getActivePlan(userId);
        if (plan.getMaxContractsPerMonth() == -1) return true;

        int currentMonth = getUsageCount(userId, "CONTRACTS");
        return currentMonth < plan.getMaxContractsPerMonth();
    }

    public boolean canCreateRental(UUID userId) {
        SubscriptionPlan plan = getActivePlan(userId);
        if (plan.getMaxRentals() == -1) return true;

        int current = rentalRepository.findByOwnerId(userId).size();
        return current < plan.getMaxRentals();
    }

    public boolean canCreateListing(UUID userId) {
        SubscriptionPlan plan = getActivePlan(userId);
        if (plan.getMaxListings() == -1) return true;

        int current = listingRepository.findByOwnerId(userId).size();
        return current < plan.getMaxListings();
    }

    public boolean hasFeature(UUID userId, String featureKey) {
        SubscriptionPlan plan = getActivePlan(userId);
        if (plan.getFeatures() == null) return false;
        Object value = plan.getFeatures().get(featureKey);
        return value instanceof Boolean && (Boolean) value;
    }

    public void assertCanCreate(UUID userId, String resourceType) {
        switch (resourceType) {
            case "PROPERTIES" -> {
                if (!canCreateProperty(userId)) {
                    throw new PlanLimitExceededException(
                            "Limite de propriétés atteinte pour votre plan. Passez au plan supérieur pour en ajouter davantage.");
                }
            }
            case "CONTRACTS" -> {
                if (!canCreateContract(userId)) {
                    throw new PlanLimitExceededException(
                            "Limite de contrats mensuels atteinte pour votre plan. Passez au plan supérieur pour créer plus de contrats.");
                }
            }
            case "RENTALS" -> {
                if (!canCreateRental(userId)) {
                    throw new PlanLimitExceededException(
                            "Limite de locations atteinte pour votre plan. Passez au plan supérieur pour en créer davantage.");
                }
            }
            case "LISTINGS" -> {
                if (!canCreateListing(userId)) {
                    throw new PlanLimitExceededException(
                            "Limite d'annonces atteinte pour votre plan. Passez au plan supérieur pour publier plus d'annonces.");
                }
            }
            default -> log.warn("Unknown resource type for feature gate: {}", resourceType);
        }
    }

    @Transactional
    public void incrementUsage(UUID userId, String metric) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime periodStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime periodEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        LocalDateTime now = LocalDateTime.now();

        UsageTracking tracking = usageTrackingRepository
                .findByUserIdAndMetricAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                        userId, metric, now, now)
                .orElseGet(() -> {
                    com.howners.gestion.domain.user.User userRef = new com.howners.gestion.domain.user.User();
                    userRef.setId(userId);
                    return UsageTracking.builder()
                            .user(userRef)
                            .metric(metric)
                            .count(0)
                            .periodStart(periodStart)
                            .periodEnd(periodEnd)
                            .build();
                });

        tracking.setCount(tracking.getCount() + 1);
        usageTrackingRepository.save(tracking);
    }

    public UsageLimitsResponse getUsageLimits(UUID userId) {
        SubscriptionPlan plan = getActivePlan(userId);
        int currentProperties = propertyRepository.findByOwnerId(userId).size();
        int currentContracts = getUsageCount(userId, "CONTRACTS");
        int currentRentals = rentalRepository.findByOwnerId(userId).size();
        int currentListings = listingRepository.findByOwnerId(userId).size();

        return new UsageLimitsResponse(
                plan.getName().name(),
                currentProperties,
                plan.getMaxProperties(),
                currentContracts,
                plan.getMaxContractsPerMonth(),
                currentRentals,
                plan.getMaxRentals(),
                currentListings,
                plan.getMaxListings(),
                plan.getMaxProperties() == -1 || currentProperties < plan.getMaxProperties(),
                plan.getMaxContractsPerMonth() == -1 || currentContracts < plan.getMaxContractsPerMonth(),
                plan.getMaxRentals() == -1 || currentRentals < plan.getMaxRentals(),
                plan.getMaxListings() == -1 || currentListings < plan.getMaxListings(),
                hasFeature(userId, "esignature"),
                hasFeature(userId, "tenant_scoring"),
                hasFeature(userId, "document_encryption"),
                hasFeature(userId, "multi_account")
        );
    }

    private SubscriptionPlan getActivePlan(UUID userId) {
        return userSubscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .map(UserSubscription::getPlan)
                .orElseGet(() -> {
                    // Default to a virtual FREE plan if no subscription exists
                    SubscriptionPlan freePlan = new SubscriptionPlan();
                    freePlan.setName(PlanName.FREE);
                    freePlan.setMaxProperties(2);
                    freePlan.setMaxContractsPerMonth(3);
                    freePlan.setMaxRentals(3);
                    freePlan.setMaxListings(2);
                    return freePlan;
                });
    }

    private int getUsageCount(UUID userId, String metric) {
        LocalDateTime now = LocalDateTime.now();
        return usageTrackingRepository
                .findByUserIdAndMetricAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                        userId, metric, now, now)
                .map(UsageTracking::getCount)
                .orElse(0);
    }
}
