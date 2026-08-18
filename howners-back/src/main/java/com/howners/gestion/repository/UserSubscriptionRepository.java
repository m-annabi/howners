package com.howners.gestion.repository;

import com.howners.gestion.domain.subscription.SubscriptionStatus;
import com.howners.gestion.domain.subscription.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    Optional<UserSubscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    /**
     * Abonnements d'un statut donné, le plus récent d'abord. Tolérant aux doublons :
     * aucune contrainte d'unicité DB ne garantit un seul abonnement ACTIVE par user,
     * donc la variante {@code Optional} peut lever une exception de cardinalité (→ 500).
     */
    List<UserSubscription> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, SubscriptionStatus status);

    Optional<UserSubscription> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<UserSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<UserSubscription> findByStripeCustomerId(String stripeCustomerId);
}
