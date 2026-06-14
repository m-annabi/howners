package com.howners.gestion.dto.email;

/**
 * Données pour un email de notification générique (template notification-generique.html).
 * messageHtml et detailsHtml acceptent du HTML simple (gras, listes) — ne jamais y injecter
 * de contenu utilisateur non échappé.
 */
public record GenericNotificationEmailData(
        String recipientEmail,
        String recipientName,
        String subject,
        String headline,
        String messageHtml,
        String detailsHtml,
        String ctaLabel,
        String ctaUrl,
        boolean urgent
) {
}
