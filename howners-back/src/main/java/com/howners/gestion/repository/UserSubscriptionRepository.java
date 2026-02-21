package com.howners.gestion.repository;

import com.howners.gestion.domain.subscription.SubscriptionStatus;
import com.howners.gestion.domain.subscription.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    Optional<UserSubscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    Optional<UserSubscription> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<UserSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<UserSubscription> findByStripeCustomerId(String stripeCustomerId);
}
