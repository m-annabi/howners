package com.howners.gestion.domain.inventory;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.rental.Rental;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Comparatif état des lieux entrée/sortie + retenues sur dépôt de garantie.
 * retenues = JSON [{piece, etatEntree, etatSortie, motif, montant}].
 */
@Entity
@Table(name = "edl_comparaisons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdlComparaison {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_id", nullable = false, unique = true)
    private Rental rental;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edl_entree_id")
    private EtatDesLieux edlEntree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edl_sortie_id")
    private EtatDesLieux edlSortie;

    @Column(name = "retenues", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String retenues;

    @Column(name = "total_retenues", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalRetenues = BigDecimal.ZERO;

    @Column(name = "solde_a_restituer", precision = 10, scale = 2)
    private BigDecimal soldeARestituer;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutComparaison statut = StatutComparaison.BROUILLON;

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
