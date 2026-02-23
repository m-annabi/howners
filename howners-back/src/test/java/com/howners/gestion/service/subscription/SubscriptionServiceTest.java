package com.howners.gestion.service.subscription;

import com.howners.gestion.domain.subscription.*;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.subscription.UserSubscriptionResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.repository.SubscriptionPlanRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.repository.UserSubscriptionRepository;
import com.howners.gestion.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionPlanRepository planRepository;
    @Mock private UserSubscriptionRepository subscriptionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private UUID userId;
    private User user;
    private SubscriptionPlan freePlan;
    private SubscriptionPlan proPlan;
    private UserSubscription activeSubscription;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("owner@test.com")
                .passwordHash("hash")
                .firstName("Jean")
                .lastName("Dupont")
                .role(Role.OWNER)
                .enabled(true)
                .build();

        freePlan = SubscriptionPlan.builder()
                .id(UUID.randomUUID())
                .name(PlanName.FREE)
                .displayName("Gratuit")
                .monthlyPrice(BigDecimal.ZERO)
                .annualPrice(BigDecimal.ZERO)
                .maxProperties(1)
                .maxContractsPerMonth(1)
                .build();

        proPlan = SubscriptionPlan.builder()
                .id(UUID.randomUUID())
                .name(PlanName.PRO)
                .displayName("Pro")
                .monthlyPrice(BigDecimal.valueOf(19.99))
                .annualPrice(BigDecimal.valueOf(199.99))
                .stripePriceIdMonthly("price_monthly_pro")
                .stripePriceIdAnnual("price_annual_pro")
                .maxProperties(10)
                .maxContractsPerMonth(10)
                .build();

        activeSubscription = UserSubscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(proPlan)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now().minusDays(15))
                .currentPeriodEnd(LocalDateTime.now().plusDays(15))
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();

        setCurrentUser(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- getAllPlans ---

    @Test
    void getAllPlans_returnsAllPlans() {
        when(planRepository.findAll()).thenReturn(List.of(freePlan, proPlan));

        var result = subscriptionService.getAllPlans();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo(PlanName.FREE);
        assertThat(result.get(1).name()).isEqualTo(PlanName.PRO);
    }

    // --- getCurrentSubscription ---

    @Test
    void getCurrentSubscription_withActiveSubscription_returnsSubscription() {
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSubscription));

        UserSubscriptionResponse result = subscriptionService.getCurrentSubscription();

        assertThat(result).isNotNull();
        assertThat(result.plan().name()).isEqualTo(PlanName.PRO);
        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void getCurrentSubscription_withNoSubscription_assignsFreePlan() {
        // First call: no subscription. After assigning free plan, second call returns it.
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(UserSubscription.builder()
                        .id(UUID.randomUUID())
                        .user(user)
                        .plan(freePlan)
                        .status(SubscriptionStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build()));
        when(planRepository.findByName(PlanName.FREE)).thenReturn(Optional.of(freePlan));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(i -> i.getArgument(0));

        UserSubscriptionResponse result = subscriptionService.getCurrentSubscription();

        assertThat(result).isNotNull();
        assertThat(result.plan().name()).isEqualTo(PlanName.FREE);
        verify(subscriptionRepository).save(any(UserSubscription.class));
    }

    // --- assignFreePlan ---

    @Test
    void assignFreePlan_whenNoExistingSubscription_assignsFreePlan() {
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(planRepository.findByName(PlanName.FREE)).thenReturn(Optional.of(freePlan));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(i -> i.getArgument(0));

        subscriptionService.assignFreePlan(userId);

        verify(subscriptionRepository).save(any(UserSubscription.class));
    }

    @Test
    void assignFreePlan_whenAlreadySubscribed_doesNothing() {
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSubscription));

        subscriptionService.assignFreePlan(userId);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void assignFreePlan_whenFreePlanNotInDb_doesNothing() {
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(planRepository.findByName(PlanName.FREE)).thenReturn(Optional.empty());

        subscriptionService.assignFreePlan(userId);

        verify(subscriptionRepository, never()).save(any());
    }

    // --- cancelSubscription ---

    @Test
    void cancelSubscription_withNoActiveSubscription_throwsException() {
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.cancelSubscription())
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No active subscription found");
    }

    // --- processSubscriptionWebhook ---

    @Test
    void processSubscriptionWebhook_deletedEvent_cancelsAndAssignsFreePlan() {
        String stripeSubId = "sub_123";
        UserSubscription sub = UserSubscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(proPlan)
                .stripeSubscriptionId(stripeSubId)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        when(subscriptionRepository.findByStripeSubscriptionId(stripeSubId))
                .thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(i -> i.getArgument(0));
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(planRepository.findByName(PlanName.FREE)).thenReturn(Optional.of(freePlan));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        subscriptionService.processSubscriptionWebhook(
                "customer.subscription.deleted", stripeSubId, "cus_123", null, null);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        verify(subscriptionRepository, atLeast(1)).save(any(UserSubscription.class));
    }

    private void setCurrentUser(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
