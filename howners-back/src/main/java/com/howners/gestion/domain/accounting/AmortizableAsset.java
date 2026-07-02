package com.howners.gestion.domain.accounting;

import com.howners.gestion.domain.property.Property;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immobilisation amortissable d'une activité LMNP (bâti, mobilier, travaux, frais).
 * L'amortissement est linéaire sur {@code durationYears} à compter de {@code startDate}.
 * Le plan d'amortissement (annuités, cumul, VNC) est calculé à la volée, non stocké.
 */
@Entity
@Table(name = "amortizable_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmortizableAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private FiscalActivity activity;

    /** Bien rattaché (optionnel : le mobilier/travaux se rapporte souvent à un bien). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AssetType type;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    /** Base amortissable (valeur d'origine hors terrain). */
    @Column(name = "base", nullable = false, precision = 12, scale = 2)
    private BigDecimal base;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "duration_years", nullable = false)
    private Integer durationYears;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
