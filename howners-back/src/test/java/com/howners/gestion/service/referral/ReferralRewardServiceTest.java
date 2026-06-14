package com.howners.gestion.service.referral;

import com.howners.gestion.domain.referral.Referral;
import com.howners.gestion.domain.referral.ReferralStatus;
import com.howners.gestion.domain.subscription.PlanName;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.repository.ReferralRepository;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.notification.NotificationService;
import com.howners.gestion.service.subscription.AbonnementActiveEvent;
import com.howners.gestion.service.subscription.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralRewardServiceTest {

    @Mock ReferralRepository referralRepository;
    @Mock SubscriptionService subscriptionService;
    @Mock NotificationService notificationService;
    @Mock EmailService emailService;

    @InjectMocks ReferralRewardService rewardService;

    UUID referrerId;
    UUID refereeId;
    Referral referral;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(rewardService, "frontendUrl", "http://localhost:4200");
        referrerId = UUID.randomUUID();
        refereeId = UUID.randomUUID();
        User referrer = User.builder().id(referrerId).email("parrain@test.fr").firstName("Pa").lastName("Rrain").build();
        User referee = User.builder().id(refereeId).email("filleul@test.fr").firstName("Fi").lastName("Lleul").build();
        referral = Referral.builder()
                .id(UUID.randomUUID())
                .referrer(referrer)
                .referee(referee)
                .status(ReferralStatus.PENDING)
                .build();
    }

    @Test
    void recompenseLesDeuxPartiesALaPremiereActivation() {
        when(referralRepository.findByRefereeId(refereeId)).thenReturn(Optional.of(referral));
        when(subscriptionService.offrirMoisPro(any())).thenReturn(true);

        rewardService.onAbonnementActive(new AbonnementActiveEvent(refereeId, PlanName.PRO, true));

        assertThat(referral.getStatus()).isEqualTo(ReferralStatus.CONVERTED);
        assertThat(referral.getConvertedAt()).isNotNull();
        assertThat(referral.getRefereeRewardedAt()).isNotNull();
        assertThat(referral.getReferrerRewardedAt()).isNotNull();
        verify(subscriptionService).offrirMoisPro(refereeId);
        verify(subscriptionService).offrirMoisPro(referrerId);
        verify(notificationService, times(2)).create(any(), any(), any(), any(), any());
        verify(referralRepository).save(referral);
    }

    @Test
    void ignoreSiPasPremiereActivation() {
        rewardService.onAbonnementActive(new AbonnementActiveEvent(refereeId, PlanName.PRO, false));

        verify(referralRepository, never()).findByRefereeId(any());
        verify(subscriptionService, never()).offrirMoisPro(any());
    }

    @Test
    void ignoreSiPasDeParrainage() {
        when(referralRepository.findByRefereeId(refereeId)).thenReturn(Optional.empty());

        rewardService.onAbonnementActive(new AbonnementActiveEvent(refereeId, PlanName.PRO, true));

        verify(subscriptionService, never()).offrirMoisPro(any());
    }

    @Test
    void estIdempotentSiDejaRecompense() {
        referral.setStatus(ReferralStatus.CONVERTED);
        referral.setConvertedAt(LocalDateTime.now().minusDays(1));
        referral.setRefereeRewardedAt(LocalDateTime.now().minusDays(1));
        referral.setReferrerRewardedAt(LocalDateTime.now().minusDays(1));
        when(referralRepository.findByRefereeId(refereeId)).thenReturn(Optional.of(referral));

        rewardService.onAbonnementActive(new AbonnementActiveEvent(refereeId, PlanName.PRO, true));

        verify(subscriptionService, never()).offrirMoisPro(any());
        verify(referralRepository, never()).save(any());
    }
}
