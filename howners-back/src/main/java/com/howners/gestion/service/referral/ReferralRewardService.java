package com.howners.gestion.service.referral;

import com.howners.gestion.domain.notification.NotificationType;
import com.howners.gestion.domain.referral.Referral;
import com.howners.gestion.domain.referral.ReferralStatus;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.GenericNotificationEmailData;
import com.howners.gestion.repository.ReferralRepository;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.notification.NotificationService;
import com.howners.gestion.service.subscription.AbonnementActiveEvent;
import com.howners.gestion.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Récompenses du programme de parrainage : à la première activation payante du filleul,
 * le parrain et le filleul reçoivent chacun 1 mois de plan PRO offert.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralRewardService {

    private final ReferralRepository referralRepository;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAbonnementActive(AbonnementActiveEvent event) {
        if (!event.premiereActivation()) {
            return;
        }
        referralRepository.findByRefereeId(event.userId())
                .filter(r -> r.getStatus() == ReferralStatus.PENDING
                        || r.getReferrerRewardedAt() == null
                        || r.getRefereeRewardedAt() == null)
                .ifPresent(this::convertirEtRecompenser);
    }

    private void convertirEtRecompenser(Referral referral) {
        if (referral.getStatus() == ReferralStatus.PENDING) {
            referral.setStatus(ReferralStatus.CONVERTED);
            referral.setConvertedAt(LocalDateTime.now());
            log.info("Referral {} converti (1er abonnement payant du filleul)", referral.getId());
        }

        if (referral.getRefereeRewardedAt() == null) {
            boolean applied = subscriptionService.offrirMoisPro(referral.getReferee().getId());
            referral.setRefereeRewardedAt(LocalDateTime.now());
            notifier(referral.getReferee(), applied,
                    "Votre récompense de parrainage est là 🎁",
                    "Merci d'avoir rejoint Howners via un parrainage ! <strong>1 mois de plan PRO offert</strong> vient d'être ajouté à votre compte.");
        }

        if (referral.getReferrerRewardedAt() == null) {
            boolean applied = subscriptionService.offrirMoisPro(referral.getReferrer().getId());
            referral.setReferrerRewardedAt(LocalDateTime.now());
            notifier(referral.getReferrer(), applied,
                    "Votre filleul s'est abonné — récompense débloquée 🎁",
                    "Votre filleul <strong>" + referral.getReferee().getFullName()
                            + "</strong> vient de souscrire un abonnement. <strong>1 mois de plan PRO offert</strong> vient d'être ajouté à votre compte.");
        }

        referralRepository.save(referral);
    }

    private void notifier(User user, boolean rewardApplied, String sujet, String messageHtml) {
        String message = rewardApplied
                ? messageHtml
                : messageHtml.replace("vient d'être ajouté à votre compte",
                        "sera appliqué sur votre prochaine facture (abonnement en cours géré par Stripe)");

        notificationService.create(
                user.getId(),
                NotificationType.REFERRAL_REWARD,
                "1 mois PRO offert",
                "Récompense de parrainage débloquée.",
                "/billing");

        emailService.sendNotificationEmail(new GenericNotificationEmailData(
                user.getEmail(),
                user.getFullName(),
                sujet,
                "Récompense de parrainage",
                message,
                null,
                "Voir mon abonnement",
                frontendUrl + "/billing",
                false
        ));
    }
}
