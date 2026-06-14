package com.howners.gestion.service.payment;

import com.howners.gestion.domain.subscription.SubscriptionStatus;
import com.howners.gestion.domain.subscription.UserSubscription;
import com.howners.gestion.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Commission plateforme dégressive selon le plan d'abonnement du propriétaire.
 */
@Service
@RequiredArgsConstructor
public class PlatformFeeService {

    private final UserSubscriptionRepository userSubscriptionRepository;

    @Value("${stripe.platform-fee-percent:2.5}")
    private double defaultFeePercent;

    @Transactional(readOnly = true)
    public BigDecimal getFeePercentPourProprietaire(UUID ownerId) {
        return userSubscriptionRepository.findByUserIdAndStatus(ownerId, SubscriptionStatus.ACTIVE)
                .map(UserSubscription::getPlan)
                .map(plan -> Optional.ofNullable(plan.getPlatformFeePercent()))
                .flatMap(fee -> fee)
                .orElseGet(() -> BigDecimal.valueOf(defaultFeePercent));
    }
}
