package com.howners.gestion.domain.contract;

import com.howners.gestion.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contract_versions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(nullable = false)
    private Integer version;

    @Column(columnDefinition = "TEXT")
    private String content;  // Contenu HTML ou JSON

    @Column(name = "document_url")
    private String documentUrl;  // URL du PDF généré (legacy, utilisez fileKey)

    @Column(name = "file_key", length = 500)
    private String fileKey;  // Clé MinIO/S3 du PDF

    @Column(name = "document_hash")
    private String documentHash;  // SHA-256 hash du PDF

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
