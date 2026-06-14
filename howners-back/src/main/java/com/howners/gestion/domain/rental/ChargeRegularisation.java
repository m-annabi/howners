package com.howners.gestion.domain.rental;

import com.howners.gestion.domain.document.Document;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Régularisation annuelle des charges locatives : provisions encaissées vs charges réelles.
 * Solde positif = complément dû par le locataire ; négatif = trop-perçu à restituer.
 */
@Entity
@Table(name = "charge_regularisations", uniqueConstraints = {
        @UniqueConstraint(name = "uq_regul_rental_annee", columnNames = {"rental_id", "annee"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeRegularisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @Column(name = "annee", nullable = false)
    private Integer annee;

    @Column(name = "provisions_encaissees", nullable = false, precision = 10, scale = 2)
    private BigDecimal provisionsEncaissees;

    @Column(name = "charges_reelles", nullable = false, precision = 10, scale = 2)
    private BigDecimal chargesReelles;

    @Column(name = "solde", nullable = false, precision = 10, scale = 2)
    private BigDecimal solde;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail")
    private Map<String, Object> detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutRegularisation statut = StatutRegularisation.BROUILLON;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
