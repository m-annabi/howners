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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

/**
 * Implémentation SMTP du service d'envoi d'emails avec Thymeleaf pour les templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${email.from:noreply@howners.com}")
    private String fromEmail;

    @Value("${email.from-name:Howners}")
    private String fromName;

    @Override
    public void sendSignatureRequestEmail(SignatureRequestEmailData data) {
        log.info("Sending signature request email to: {}", data.recipientEmail());

        try {
            Context context = new Context();
            context.setVariable("recipientName", data.recipientName());
            context.setVariable("ownerName", data.ownerName());
            context.setVariable("propertyName", data.propertyName());
            context.setVariable("propertyAddress", data.propertyAddress());
            context.setVariable("contractNumber", data.contractNumber());
            context.setVariable("signingUrl", data.signingUrl());
            context.setVariable("expirationDate", data.expirationDate());

            String htmlContent = templateEngine.process("email/signature-request", context);

            sendHtmlEmail(
                    data.recipientEmail(),
                    "Signature de votre contrat de location - " + data.propertyName(),
                    htmlContent
            );

            log.info("Signature request email sent successfully to: {}", data.recipientEmail());
        } catch (Exception e) {
            log.error("Failed to send signature request email to: {}", data.recipientEmail(), e);
            throw new RuntimeException("Failed to send signature request email", e);
        }
    }

    @Override
    public void sendSignatureCompletedEmail(SignatureCompletedEmailData data) {
        log.info("Sending signature completed email to: {}", data.recipientEmail());

        try {
            Context context = new Context();
            context.setVariable("recipientName", data.recipientName());
            context.setVariable("tenantName", data.tenantName());
            context.setVariable("propertyName", data.propertyName());
            context.setVariable("contractNumber", data.contractNumber());
            context.setVariable("signedDate", data.signedDate());
            context.setVariable("contractViewUrl", data.contractViewUrl());

            String htmlContent = templateEngine.process("email/signature-completed", context);

            sendHtmlEmail(
                    data.recipientEmail(),
                    "Contrat signé - " + data.propertyName(),
                    htmlContent
            );

            log.info("Signature completed email sent successfully to: {}", data.recipientEmail());
        } catch (Exception e) {
            log.error("Failed to send signature completed email to: {}", data.recipientEmail(), e);
            throw new RuntimeException("Failed to send signature completed email", e);
        }
    }

    @Override
    public void sendSignatureDeclinedEmail(SignatureDeclinedEmailData data) {
        log.info("Sending signature declined email to: {}", data.recipientEmail());

        try {
            Context context = new Context();
            context.setVariable("recipientName", data.recipientName());
            context.setVariable("tenantName", data.tenantName());
            context.setVariable("propertyName", data.propertyName());
            context.setVariable("contractNumber", data.contractNumber());
            context.setVariable("declinedDate", data.declinedDate());
            context.setVariable("reason", data.reason());

            String htmlContent = templateEngine.process("email/signature-declined", context);

            sendHtmlEmail(
                    data.recipientEmail(),
                    "Contrat refusé - " + data.propertyName(),
                    htmlContent
            );

            log.info("Signature declined email sent successfully to: {}", data.recipientEmail());
        } catch (Exception e) {
            log.error("Failed to send signature declined email to: {}", data.recipientEmail(), e);
            throw new RuntimeException("Failed to send signature declined email", e);
        }
    }

    @Override
    public void sendWelcomeTenantEmail(WelcomeTenantEmailData data) {
        log.info("Sending welcome tenant email to: {}", data.recipientEmail());

        try {
            Context context = new Context();
            context.setVariable("recipientName", data.recipientName());
            context.setVariable("recipientEmail", data.recipientEmail());
            context.setVariable("ownerName", data.ownerName());
            context.setVariable("propertyName", data.propertyName());
            context.setVariable("tempPassword", data.tempPassword());
            context.setVariable("loginUrl", data.loginUrl());

            String htmlContent = templateEngine.process("email/welcome-tenant", context);

            sendHtmlEmail(
                    data.recipientEmail(),
                    "Bienvenue sur Howners - Vos identifiants de connexion",
                    htmlContent
            );

            log.info("Welcome tenant email sent successfully to: {}", data.recipientEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome tenant email to: {}", data.recipientEmail(), e);
            throw new RuntimeException("Failed to send welcome tenant email", e);
        }
    }

    @Override
    public void sendWelcomeOwnerEmail(WelcomeOwnerEmailData data) {
        log.info("Sending welcome owner email to: {}", data.recipientEmail());

        try {
            Context context = new Context();
            context.setVariable("recipientName", data.recipientName());
            context.setVariable("dashboardUrl", data.dashboardUrl());
            context.setVariable("addPropertyUrl", data.addPropertyUrl());
            context.setVariable("pricingUrl", data.pricingUrl());

            String htmlContent = templateEngine.process("email/welcome-owner", context);

            sendHtmlEmail(
                    data.recipientEmail(),
                    "Bienvenue sur Howners — démarrez en 3 minutes",
                    htmlContent
            );

            log.info("Welcome owner email sent successfully to: {}", data.recipientEmail());
        } catch (Exception e) {
            // Don't fail registration on email failure — log and move on.
            log.error("Failed to send welcome owner email to {}: {}", data.recipientEmail(), e.getMessage());
        }
    }

    @Override
    public void sendReceiptEmail(ReceiptEmailData data) {
        log.info("Sending receipt email to: {}", data.recipientEmail());

        try {
            Context context = new Context();
            context.setVariable("recipientName", data.recipientName());
            context.setVariable("ownerName", data.ownerName());
            context.setVariable("propertyName", data.propertyName());
            context.setVariable("propertyAddress", data.propertyAddress());
            context.setVariable("receiptNumber", data.receiptNumber());
            context.setVariable("periodLabel", data.periodLabel());
            context.setVariable("totalAmount", data.totalAmount());
            context.setVariable("currency", data.currency());
            context.setVariable("receiptViewUrl", data.receiptViewUrl());

            String htmlContent = templateEngine.process("email/receipt-generated", context);

            sendHtmlEmail(
                    data.recipientEmail(),
                    "Votre quittance de loyer - " + data.periodLabel(),
                    htmlContent
            );

            log.info("Receipt email sent successfully to: {}", data.recipientEmail());
        } catch (Exception e) {
            log.error("Failed to send receipt email to: {}", data.recipientEmail(), e);
            throw new RuntimeException("Failed to send receipt email", e);
        }
    }

    @Override
    public void sendApplicationAcceptedEmail(ApplicationReviewedEmailData data) {
        log.info("Sending application accepted email to: {}", data.recipientEmail());

        try {
            Context context = new Context();
            context.setVariable("recipientName", data.recipientName());
            context.setVariable("ownerName", data.ownerName());
            context.setVariable("propertyName", data.propertyName());
            context.setVariable("listingTitle", data.listingTitle());
            context.setVariable("notes", data.notes());
            context.setVariable("dashboardUrl", data.dashboardUrl());

            String htmlContent = templateEngine.process("email/application-accepted", context);

            sendHtmlEmail(
                    data.recipientEmail(),
                    "Candidature acceptée - " + data.propertyName(),
                    htmlContent
            );

            log.info("Application accepted email sent successfully to: {}", data.recipientEmail());
        } catch (Exception e) {
            log.error("Failed to send application accepted email to: {}", data.recipientEmail(), e);
        }
    }

    @Override
    public void sendApplicationRejectedEmail(ApplicationReviewedEmailData data) {
        log.info("Sending application rejected email to: {}", data.recipientEmail());

        try {
            Context context = new Context();
            context.setVariable("recipientName", data.recipientName());
            context.setVariable("ownerName", data.ownerName());
            context.setVariable("propertyName", data.propertyName());
            context.setVariable("listingTitle", data.listingTitle());
            context.setVariable("notes", data.notes());
            context.setVariable("dashboardUrl", data.dashboardUrl());

            String htmlContent = templateEngine.process("email/application-rejected", context);

            sendHtmlEmail(
                    data.recipientEmail(),
                    "Réponse à votre candidature - " + data.propertyName(),
                    htmlContent
            );

            log.info("Application rejected email sent successfully to: {}", data.recipientEmail());
        } catch (Exception e) {
            log.error("Failed to send application rejected email to: {}", data.recipientEmail(), e);
        }
    }

    @Override
    public void sendWeeklyDigestEmail(WeeklyDigestEmailData data) {
        log.info("Sending weekly digest to: {}", data.recipientEmail());
        try {
            Context context = new Context();
            context.setVariable("ownerName", data.ownerName());
            context.setVariable("latePayments", data.latePayments());
            context.setVariable("expiringContracts", data.expiringContracts());
            context.setVariable("awaitingSignatures", data.awaitingSignatures());
            context.setVariable("pendingApplications", data.pendingApplications());
            context.setVariable("dashboardUrl", data.dashboardUrl());

            String htmlContent = templateEngine.process("email/weekly-digest", context);

            sendHtmlEmail(
                    data.recipientEmail(),
                    "À traiter cette semaine sur Howners",
                    htmlContent
            );
            log.info("Weekly digest sent to: {}", data.recipientEmail());
        } catch (Exception e) {
            log.error("Failed to send weekly digest to {}: {}", data.recipientEmail(), e.getMessage());
        }
    }

    @Override
    public void sendPaymentReminderEmail(PaymentReminderEmailData data) {
        log.info("Sending payment reminder email to: {}", data.recipientEmail());

        try {
            Context context = new Context();
            context.setVariable("recipientName", data.recipientName());
            context.setVariable("ownerName", data.ownerName());
            context.setVariable("propertyName", data.propertyName());
            context.setVariable("propertyAddress", data.propertyAddress());
            context.setVariable("amount", data.amount());
            context.setVariable("currency", data.currency());
            context.setVariable("dueDate", data.dueDate());
            context.setVariable("paymentUrl", data.paymentUrl());
            context.setVariable("isOverdue", data.isOverdue());

            String htmlContent = templateEngine.process("email/payment-reminder", context);

            sendHtmlEmail(
                    data.recipientEmail(),
                    "Rappel de loyer - " + data.propertyName(),
                    htmlContent
            );

            log.info("Payment reminder email sent successfully to: {}", data.recipientEmail());
        } catch (Exception e) {
            log.error("Failed to send payment reminder email to: {}", data.recipientEmail(), e);
            throw new RuntimeException("Failed to send payment reminder email", e);
        }
    }

    @Override
    public void sendOnboardingReminderEmail(OnboardingReminderEmailData data) {
        log.info("Sending onboarding reminder email to: {}", data.recipientEmail());

        try {
            Context context = new Context();
            context.setVariable("recipientName", data.recipientName());
            context.setVariable("addPropertyUrl", data.addPropertyUrl());
            context.setVariable("dashboardUrl", data.dashboardUrl());

            String htmlContent = templateEngine.process("email/onboarding-reminder", context);

            sendHtmlEmail(
                    data.recipientEmail(),
                    "N'oubliez pas d'ajouter votre premier bien",
                    htmlContent
            );

            log.info("Onboarding reminder email sent successfully to: {}", data.recipientEmail());
        } catch (Exception e) {
            log.error("Failed to send onboarding reminder email to {}: {}", data.recipientEmail(), e.getMessage());
        }
    }

    @Override
    public void sendContractExpiryWarningEmail(ContractExpiryEmailData data) {
        log.info("Sending contract expiry warning email to: {}", data.recipientEmail());

        try {
            Context context = new Context();
            context.setVariable("recipientName", data.recipientName());
            context.setVariable("tenantName", data.tenantName());
            context.setVariable("propertyName", data.propertyName());
            context.setVariable("contractNumber", data.contractNumber());
            context.setVariable("expiryDate", data.expiryDate());
            context.setVariable("contractViewUrl", data.contractViewUrl());

            String htmlContent = templateEngine.process("email/contract-expiry", context);

            sendHtmlEmail(
                    data.recipientEmail(),
                    "Contrat arrivant a echeance - " + data.propertyName(),
                    htmlContent
            );

            log.info("Contract expiry warning email sent successfully to: {}", data.recipientEmail());
        } catch (Exception e) {
            log.error("Failed to send contract expiry warning email to {}: {}", data.recipientEmail(), e.getMessage());
        }
    }

    /**
     * Envoie un email HTML
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
