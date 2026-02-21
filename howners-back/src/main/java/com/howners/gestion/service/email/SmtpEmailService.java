package com.howners.gestion.service.email;

import com.howners.gestion.dto.email.ReceiptEmailData;
import com.howners.gestion.dto.email.SignatureCompletedEmailData;
import com.howners.gestion.dto.email.SignatureDeclinedEmailData;
import com.howners.gestion.dto.email.SignatureRequestEmailData;
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
