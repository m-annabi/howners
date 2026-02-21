package com.howners.gestion.domain.signature;

import com.howners.gestion.domain.contract.Contract;
import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "signatures")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signer_id", nullable = false)
    private User signer;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_type", nullable = false)
    private SignatureType signatureType;

    private String provider;  // YOUSIGN, DOCUSIGN, INTERNAL, etc.

    @Column(name = "provider_signature_id")
    private String providerSignatureId;

    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData;  // Base64 pour signature simple

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignatureStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
