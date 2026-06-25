package com.howners.gestion.service.subscription;

import com.howners.gestion.domain.subscription.PlanName;
import com.howners.gestion.domain.subscription.SubscriptionPlan;
import com.howners.gestion.domain.subscription.SubscriptionStatus;
import com.howners.gestion.domain.subscription.UserSubscription;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.repository.SubscriptionPlanRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionPlanRepository planRepository;
    @Mock private UserSubscriptionRepository subscriptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private UUID userId;
    private User user;
    private SubscriptionPlan proPlan;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("owner@test.com")
                .firstName("Marie").lastName("Martin").passwordHash("h").role(Role.OWNER).enabled(true).build();
        proPlan = SubscriptionPlan.builder().id(UUID.randomUUID()).name(PlanName.PRO).displayName("Pro").build();
    }

    private UserSubscription activeSub(SubscriptionPlan plan, String stripeSubId) {
        return UserSubscription.builder()
                .id(UUID.randomUUID()).user(user).plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .stripeSubscriptionId(stripeSubId)
                .build();
    }

    // --- Webhook : activation d'abonnement ---

    @Test
    void processSubscriptionWebhook_created_activatesMapsPlanAndPublishesEvent() {
        UserSubscription existing = UserSubscription.builder()
                .id(UUID.randomUUID()).user(user).plan(null)
                .status(SubscriptionStatus.ACTIVE).stripeCustomerId("cus_1").build();

        when(subscriptionRepository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.empty());
        when(subscriptionRepository.findByStripeCustomerId("cus_1")).thenReturn(Optional.of(existing));
        when(planRepository.findByStripePriceIdMonthlyOrStripePriceIdAnnual("price_1", "price_1"))
                .thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.processSubscriptionWebhook(
                "customer.subscription.created", "sub_1", "cus_1", "price_1", 1_700_000_000L, 1_702_592_000L);

        assertThat(existing.getPlan()).isEqualTo(proPlan);
        assertThat(existing.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(existing.getStripeSubscriptionId()).isEqualTo("sub_1");

        ArgumentCaptor<AbonnementActiveEvent> captor = ArgumentCaptor.forClass(AbonnementActiveEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().plan()).isEqualTo(PlanName.PRO);
        assertThat(captor.getValue().premiereActivation()).isTrue();
        assertThat(captor.getValue().userId()).isEqualTo(userId);
    }

    @Test
    void processSubscriptionWebhook_deleted_setsCancelledAndAssignsFree() {
        UserSubscription stripeSub = activeSub(proPlan, "sub_1");
        SubscriptionPlan freePlan = SubscriptionPlan.builder().id(UUID.randomUUID())
                .name(PlanName.FREE).displayName("Gratuit").build();

        when(subscriptionRepository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(stripeSub));
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(planRepository.findByName(PlanName.FREE)).thenReturn(Optional.of(freePlan));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.processSubscriptionWebhook(
                "customer.subscription.deleted", "sub_1", "cus_1", null, null, null);

        assertThat(stripeSub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        verify(eventPublisher, never()).publishEvent(any());
    }

    // --- Récompense parrainage (chemins non-Stripe) ---

    @Test
    void offrirMoisPro_freeUser_createsProRewardForOneMonth() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(planRepository.findByName(PlanName.PRO)).thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = subscriptionService.offrirMoisPro(userId);

        assertThat(result).isTrue();
        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getPlan()).isEqualTo(proPlan);
        assertThat(captor.getValue().getCancelAtPeriodEnd()).isTrue();
        assertThat(captor.getValue().getCurrentPeriodEnd()).isAfter(LocalDateTime.now().plusDays(27));
    }

    @Test
    void offrirMoisPro_paidNonStripe_extendsPeriodByOneMonth() {
        UserSubscription current = activeSub(proPlan, null);
        LocalDateTime end = LocalDateTime.now().plusDays(10);
        current.setCurrentPeriodEnd(end);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(current));
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = subscriptionService.offrirMoisPro(userId);

        assertThat(result).isTrue();
        assertThat(current.getCurrentPeriodEnd()).isEqualTo(end.plusMonths(1));
    }

    @Test
    void offrirMoisPro_stripeSubWithoutStripeConfig_returnsFalse() {
        UserSubscription current = activeSub(proPlan, "sub_live_1");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(current));

        boolean result = subscriptionService.offrirMoisPro(userId);

        assertThat(result).isFalse();
        verify(subscriptionRepository, never()).save(any());
    }

    // --- Bascule directe (mode local sans Stripe) ---

    @Test
    void switchPlanDirectly_createsActiveSubAndPublishesEvent() {
        when(subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.empty());
        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.switchPlanDirectly(userId, user, proPlan, "monthly");

        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getPlan()).isEqualTo(proPlan);
        assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        ArgumentCaptor<AbonnementActiveEvent> ev = ArgumentCaptor.forClass(AbonnementActiveEvent.class);
        verify(eventPublisher).publishEvent(ev.capture());
        assertThat(ev.getValue().premiereActivation()).isTrue();
        assertThat(ev.getValue().plan()).isEqualTo(PlanName.PRO);
    }
}
