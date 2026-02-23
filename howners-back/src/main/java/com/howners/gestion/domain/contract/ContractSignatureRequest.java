package com.howners.gestion.domain.contract;

import com.howners.gestion.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant une demande de signature électronique pour un contrat
 */
@Entity
@Table(name = "contract_signature_requests", indexes = {
        @Index(name = "idx_contract_id", columnList = "contract_id"),
        @Index(name = "idx_access_token", columnList = "access_token"),
        @Index(name = "idx_signer_id", columnList = "signer_id"),
        @Index(name = "idx_provider_envelope_id", columnList = "provider_envelope_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractSignatureRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Le contrat à signer
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    /**
     * Le fournisseur de signature électronique (DIRECT)
     */
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    /**
     * L'identifiant de l'enveloppe/document chez le fournisseur
     */
    @Column(name = "provider_envelope_id", length = 255)
    private String providerEnvelopeId;

    /**
     * Le signataire (locataire)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signer_id", nullable = false)
    private User signer;

    /**
     * Email du signataire (stocké pour historique même si l'utilisateur change d'email)
     */
    @Column(name = "signer_email", nullable = false)
    private String signerEmail;

    /**
     * Token d'accès unique (hashé avec BCrypt)
     */
    @Column(name = "access_token", nullable = false, unique = true, length = 255)
    private String accessToken;

    /**
     * Date d'expiration du token
     */
    @Column(name = "token_expires_at", nullable = false)
    private LocalDateTime tokenExpiresAt;

    /**
     * Statut de la demande de signature
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SignatureRequestStatus status;

    /**
     * URL de signature (fournie par le provider)
     */
    @Column(name = "signing_url", length = 1000)
    private String signingUrl;

    /**
     * Date d'envoi de l'email
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * Date à laquelle le signataire a consulté le contrat
     */
    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    /**
     * Date de signature
     */
    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    /**
     * Date de refus
     */
    @Column(name = "declined_at")
    private LocalDateTime declinedAt;

    /**
     * Adresse IP du signataire (pour audit)
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent du signataire (pour audit)
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Raison du refus (si applicable)
     */
    @Column(name = "decline_reason", length = 1000)
    private String declineReason;

    /**
     * Nombre de fois que l'email a été renvoyé
     */
    @Column(name = "resend_count", nullable = false)
    @Builder.Default
    private Integer resendCount = 0;

    /**
     * Date de la dernière tentative de renvoi
     */
    @Column(name = "last_resent_at")
    private LocalDateTime lastResentAt;

    /**
     * Nombre de rappels automatiques envoyés
     */
    @Column(name = "reminder_count", nullable = false)
    @Builder.Default
    private Integer reminderCount = 0;

    /**
     * Date du dernier rappel automatique
     */
    @Column(name = "last_reminder_at")
    private LocalDateTime lastReminderAt;

    /**
     * Ordre de signature (pour multi-signature séquentielle)
     */
    @Column(name = "signer_order", nullable = false)
    @Builder.Default
    private Integer signerOrder = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Vérifie si le token est expiré
     */
    public boolean isTokenExpired() {
        return LocalDateTime.now().isAfter(tokenExpiresAt);
    }

    /**
     * Vérifie si la demande peut être renvoyée
     */
    public boolean canBeResent() {
        return status == SignatureRequestStatus.SENT ||
               status == SignatureRequestStatus.VIEWED ||
               status == SignatureRequestStatus.EXPIRED;
    }

    /**
     * Vérifie si la demande peut être annulée
     */
    public boolean canBeCancelled() {
        return status == SignatureRequestStatus.PENDING ||
               status == SignatureRequestStatus.SENT ||
               status == SignatureRequestStatus.VIEWED;
    }
}
