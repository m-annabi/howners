package com.howners.gestion.service.email;

import com.howners.gestion.dto.email.ApplicationReviewedEmailData;
import com.howners.gestion.dto.email.ContractExpiryEmailData;
import com.howners.gestion.dto.email.OnboardingReminderEmailData;
import com.howners.gestion.dto.email.PaymentReminderEmailData;
import com.howners.gestion.dto.email.ReceiptEmailData;
import com.howners.gestion.dto.email.SignatureCompletedEmailData;
import com.howners.gestion.dto.email.SignatureDeclinedEmailData;
import com.howners.gestion.dto.email.SignatureRequestEmailData;
import com.howners.gestion.dto.email.WeeklyDigestEmailData;
import com.howners.gestion.dto.email.WelcomeOwnerEmailData;
import com.howners.gestion.dto.email.WelcomeTenantEmailData;

/**
 * Service d'envoi d'emails
 */
public interface EmailService {

    /**
     * Envoie un email de demande de signature au locataire
     */
    void sendSignatureRequestEmail(SignatureRequestEmailData data);

    /**
     * Envoie un email de confirmation au propriétaire quand le contrat est signé
     */
    void sendSignatureCompletedEmail(SignatureCompletedEmailData data);

    /**
     * Envoie un email au propriétaire quand le contrat est refusé
     */
    void sendSignatureDeclinedEmail(SignatureDeclinedEmailData data);

    /**
     * Envoie un email de bienvenue au nouveau locataire avec ses identifiants
     */
    void sendWelcomeTenantEmail(WelcomeTenantEmailData data);

    /**
     * Envoie un email de bienvenue à un nouveau bailleur après inscription
     */
    void sendWelcomeOwnerEmail(WelcomeOwnerEmailData data);

    /**
     * Envoie un email de notification au locataire quand sa quittance de loyer est générée
     */
    void sendReceiptEmail(ReceiptEmailData data);

    /**
     * Envoie un email au candidat quand sa candidature est acceptée
     */
    void sendApplicationAcceptedEmail(ApplicationReviewedEmailData data);

    /**
     * Envoie un email au candidat quand sa candidature est refusée
     */
    void sendApplicationRejectedEmail(ApplicationReviewedEmailData data);

    /**
     * Envoie le digest hebdomadaire "À traiter" au bailleur.
     */
    void sendWeeklyDigestEmail(WeeklyDigestEmailData data);

    /**
     * Envoie un email de rappel de paiement au locataire
     */
    void sendPaymentReminderEmail(PaymentReminderEmailData data);

    /**
     * Envoie un email de relance d'onboarding (J+2) au bailleur sans bien
     */
    void sendOnboardingReminderEmail(OnboardingReminderEmailData data);

    /**
     * Envoie un email d'alerte d'echeance de contrat (J-30) au bailleur
     */
    void sendContractExpiryWarningEmail(ContractExpiryEmailData data);
}
