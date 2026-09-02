package com.howners.gestion.service.notification;

import com.howners.gestion.domain.notification.NotificationType;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.GenericNotificationEmailData;
import com.howners.gestion.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Point d'entrée unique « notification in-app + e-mail » pour les services métier.
 * Factorise le couple notificationService.create(...) / emailService.sendNotificationEmail(...)
 * et la garde « destinataire nul ou sans adresse » répétée dans chaque service.
 * L'envoi d'e-mail reste best-effort (voir SmtpEmailService) : il ne fait jamais échouer le flux appelant.
 */
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationService notificationService;
    private final EmailService emailService;

    /** Contenu d'un e-mail générique (template notification-generique.html), sans le destinataire. */
    public record Email(String subject, String headline, String messageHtml, String detailsHtml,
                        String ctaLabel, String ctaUrl, boolean urgent) {

        public static Email of(String subject, String headline, String messageHtml) {
            return new Email(subject, headline, messageHtml, null, null, null, false);
        }
    }

    /** Notification in-app seule (ignorée si le destinataire est nul). */
    public void notify(User recipient, NotificationType type, String title, String message, String link) {
        if (recipient == null) return;
        notificationService.create(recipient.getId(), type, title, message, link);
    }

    /** E-mail seul (ignoré si le destinataire est nul ou sans adresse). */
    public void email(User recipient, Email email) {
        if (recipient == null || recipient.getEmail() == null || email == null) return;
        emailService.sendNotificationEmail(new GenericNotificationEmailData(
                recipient.getEmail(), recipient.getFullName(),
                email.subject(), email.headline(), email.messageHtml(), email.detailsHtml(),
                email.ctaLabel(), email.ctaUrl(), email.urgent()));
    }

    /** Notification in-app puis e-mail au même destinataire. */
    public void notifyAndEmail(User recipient, NotificationType type, String title, String message, String link,
                               Email email) {
        notify(recipient, type, title, message, link);
        email(recipient, email);
    }
}
