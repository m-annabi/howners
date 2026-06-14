package com.howners.gestion.domain.rental;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Indice de référence des loyers (IRL) publié trimestriellement par l'INSEE.
 */
@Entity
@Table(name = "irl_indices", uniqueConstraints = {
        @UniqueConstraint(name = "uq_irl_annee_trimestre", columnNames = {"annee", "trimestre"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrlIndice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "annee", nullable = false)
    private Integer annee;

    @Column(name = "trimestre", nullable = false)
    private Integer trimestre;

    @Column(name = "valeur", nullable = false, precision = 6, scale = 2)
    private BigDecimal valeur;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
