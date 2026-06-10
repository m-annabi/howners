package com.howners.gestion.service.onboarding;

import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.OnboardingReminderEmailData;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service planifie pour l'envoi d'emails de relance d'onboarding (J+2).
 * Cible les bailleurs inscrits entre 48h et 72h qui n'ont pas encore ajoute de bien.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingReminderService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Execute chaque jour a 10h00. Recherche les bailleurs inscrits entre 48h et 72h
     * qui n'ont aucun bien enregistre et leur envoie un email de relance.
     */
    @Scheduled(cron = "0 0 10 * * ?")
    @Transactional(readOnly = true)
    public void sendOnboardingReminders() {
        log.info("Demarrage du job de relance d'onboarding (J+2)");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusHours(72);
        LocalDateTime to = now.minusHours(48);

        List<User> owners = userRepository.findOwnersCreatedBetween(from, to);
        log.info("{} bailleur(s) inscrit(s) entre 48h et 72h trouves", owners.size());

        int sent = 0;
        int skipped = 0;

        for (User owner : owners) {
            try {
                boolean hasProperties = !propertyRepository.findByOwnerId(owner.getId()).isEmpty();

                if (hasProperties) {
                    skipped++;
                    continue;
                }

                String recipientName = owner.getFirstName() != null ? owner.getFirstName() : owner.getFullName();

                OnboardingReminderEmailData data = OnboardingReminderEmailData.builder()
                        .recipientEmail(owner.getEmail())
                        .recipientName(recipientName)
                        .addPropertyUrl(frontendUrl + "/properties/new")
                        .dashboardUrl(frontendUrl + "/dashboard")
                        .build();

                emailService.sendOnboardingReminderEmail(data);
                sent++;
                log.info("Email de relance d'onboarding envoye a : {}", owner.getEmail());
            } catch (Exception e) {
                log.error("Echec de l'envoi de la relance d'onboarding a {} : {}", owner.getEmail(), e.getMessage());
            }
        }

        log.info("Job de relance d'onboarding termine — {} envoye(s), {} ignore(s) (ont deja un bien)", sent, skipped);
    }
}
