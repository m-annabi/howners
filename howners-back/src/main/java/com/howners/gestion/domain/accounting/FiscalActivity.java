package com.howners.gestion.domain.accounting;

import com.howners.gestion.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Activité fiscale d'un bailleur (une par propriétaire, régime + juridiction).
 * En LMNP, la déclaration BIC est portée au niveau du foyer : l'activité regroupe
 * l'ensemble des biens meublés du propriétaire. Le bilan d'ouverture (apport +
 * trésorerie initiale) sert à équilibrer le premier exercice.
 */
@Entity
@Table(name = "fiscal_activities", uniqueConstraints = {
        @UniqueConstraint(name = "uq_fiscal_activity_owner", columnNames = {"owner_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "jurisdiction", nullable = false, length = 20)
    @Builder.Default
    private FiscalJurisdiction jurisdiction = FiscalJurisdiction.FR;

    @Enumerated(EnumType.STRING)
    @Column(name = "regime", nullable = false, length = 30)
    @Builder.Default
    private FiscalRegime regime = FiscalRegime.LMNP_REEL;

    /** Début d'activité (date de mise en location meublée). */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Trésorerie d'ouverture (à-nouveau) pour équilibrer le bilan du 1er exercice. */
    @Column(name = "opening_cash", precision = 12, scale = 2)
    private BigDecimal openingCash;

    /** Apport initial de l'exploitant (compte 108) — bilan d'ouverture. */
    @Column(name = "apport_initial", precision = 12, scale = 2)
    private BigDecimal apportInitial;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
