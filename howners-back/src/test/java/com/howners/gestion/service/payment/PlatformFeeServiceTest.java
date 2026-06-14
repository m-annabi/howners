package com.howners.gestion.service.payment;

import com.howners.gestion.domain.subscription.PlanName;
import com.howners.gestion.domain.subscription.SubscriptionPlan;
import com.howners.gestion.domain.subscription.SubscriptionStatus;
import com.howners.gestion.domain.subscription.UserSubscription;
import com.howners.gestion.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformFeeServiceTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @InjectMocks
    private PlatformFeeService platformFeeService;

    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(platformFeeService, "defaultFeePercent", 2.5);
    }

    @Test
    void retourneLaCommissionDuPlanActif() {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(PlanName.PRO)
                .platformFeePercent(new BigDecimal("1.5"))
                .build();
        UserSubscription sub = UserSubscription.builder().plan(plan).build();
        when(userSubscriptionRepository.findByUserIdAndStatus(ownerId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(sub));

        assertThat(platformFeeService.getFeePercentPourProprietaire(ownerId))
                .isEqualByComparingTo("1.5");
    }

    @Test
    void retourneLeFallbackSansAbonnement() {
        when(userSubscriptionRepository.findByUserIdAndStatus(ownerId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThat(platformFeeService.getFeePercentPourProprietaire(ownerId))
                .isEqualByComparingTo("2.5");
    }

    @Test
    void retourneLeFallbackSiCommissionDuPlanNulle() {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(PlanName.FREE)
                .platformFeePercent(null)
                .build();
        UserSubscription sub = UserSubscription.builder().plan(plan).build();
        when(userSubscriptionRepository.findByUserIdAndStatus(ownerId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(sub));

        assertThat(platformFeeService.getFeePercentPourProprietaire(ownerId))
                .isEqualByComparingTo("2.5");
    }
}
