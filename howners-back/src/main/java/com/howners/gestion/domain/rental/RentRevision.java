package com.howners.gestion.domain.rental;

import com.howners.gestion.domain.document.Document;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Révision annuelle de loyer indexée sur l'IRL (article 17-1 de la loi n° 89-462).
 */
@Entity
@Table(name = "rent_revisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @Column(name = "ancien_loyer", nullable = false, precision = 10, scale = 2)
    private BigDecimal ancienLoyer;

    @Column(name = "nouveau_loyer", nullable = false, precision = 10, scale = 2)
    private BigDecimal nouveauLoyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indice_ancien_id")
    private IrlIndice indiceAncien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indice_nouveau_id")
    private IrlIndice indiceNouveau;

    @Column(name = "date_revision", nullable = false)
    private LocalDate dateRevision;

    @Column(name = "date_effet")
    private LocalDate dateEffet;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutRevision statut = StatutRevision.BROUILLON;

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
