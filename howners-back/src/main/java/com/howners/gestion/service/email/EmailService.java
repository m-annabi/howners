package com.howners.gestion.service.email;

import com.howners.gestion.dto.email.ReceiptEmailData;
import com.howners.gestion.dto.email.SignatureCompletedEmailData;
import com.howners.gestion.dto.email.SignatureDeclinedEmailData;
import com.howners.gestion.dto.email.SignatureRequestEmailData;
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
     * Envoie un email de notification au locataire quand sa quittance de loyer est générée
     */
    void sendReceiptEmail(ReceiptEmailData data);
}
