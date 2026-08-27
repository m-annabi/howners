package com.howners.gestion.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String phone;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "postal_code")
    private String postalCode;

    private String city;

    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Boolean enabled = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "anonymized_at")
    private LocalDateTime anonymizedAt;

    @Column(name = "is_anonymized")
    private Boolean isAnonymized = false;

    // Vérification d'e-mail : tant que false, la connexion est refusée. Le hash SHA-256 du token de
    // vérification (jamais le token en clair) et son expiration sont stockés ici le temps de la vérif.
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "email_verification_token_hash", length = 64)
    private String emailVerificationTokenHash;

    @Column(name = "email_verification_expires_at")
    private LocalDateTime emailVerificationExpiresAt;

    @Column(name = "referral_code", unique = true, length = 20)
    private String referralCode;

    @Column(name = "stripe_connect_account_id", length = 255)
    private String stripeConnectAccountId;

    @Column(name = "stripe_connect_status", length = 40)
    @Builder.Default
    private String stripeConnectStatus = "NONE";

    // Version de jeton : incrémentée pour révoquer d'un coup tous les JWT existants de l'utilisateur
    // (« déconnexion de toutes les sessions »). Le JWT embarque cette valeur ; le filtre la compare
    // à celle en base et rejette tout jeton dont la version est périmée.
    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private Integer tokenVersion = 0;

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}
