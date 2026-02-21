package com.howners.gestion.domain.inventory;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "etat_des_lieux")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtatDesLieux {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EtatDesLieuxType type;

    @Column(name = "inspection_date", nullable = false)
    private LocalDate inspectionDate;

    @Column(name = "room_conditions", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String roomConditions;

    @Column(name = "meter_readings", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String meterReadings;

    @Column(name = "keys_count")
    private Integer keysCount;

    @Column(name = "keys_description", columnDefinition = "TEXT")
    private String keysDescription;

    @Column(name = "general_comments", columnDefinition = "TEXT")
    private String generalComments;

    @Column(name = "owner_signed")
    @Builder.Default
    private Boolean ownerSigned = false;

    @Column(name = "tenant_signed")
    @Builder.Default
    private Boolean tenantSigned = false;

    @Column(name = "owner_signed_at")
    private LocalDateTime ownerSignedAt;

    @Column(name = "tenant_signed_at")
    private LocalDateTime tenantSignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
