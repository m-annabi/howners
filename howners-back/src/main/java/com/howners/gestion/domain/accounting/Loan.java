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
 * Emprunt d'une activité LMNP (amortissement à échéance constante). En LMNP réel, les
 * intérêts d'emprunt et l'assurance emprunteur sont déductibles ; le capital remboursé
 * ne l'est pas (il diminue la dette au passif).
 */
@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private FiscalActivity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    /** Capital emprunté (montant initial). */
    @Column(name = "principal", nullable = false, precision = 12, scale = 2)
    private BigDecimal principal;

    /** Taux nominal annuel en pourcentage (ex. 3.20). */
    @Column(name = "annual_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal annualRate;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Assurance emprunteur mensuelle (déductible). Null = 0. */
    @Column(name = "insurance_monthly", precision = 10, scale = 2)
    private BigDecimal insuranceMonthly;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
