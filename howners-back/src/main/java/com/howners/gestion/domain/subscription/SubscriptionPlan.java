package com.howners.gestion.domain.subscription;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true)
    private PlanName name;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "stripe_price_id_monthly")
    private String stripePriceIdMonthly;

    @Column(name = "stripe_price_id_annual")
    private String stripePriceIdAnnual;

    @Column(name = "monthly_price", nullable = false)
    @Builder.Default
    private BigDecimal monthlyPrice = BigDecimal.ZERO;

    @Column(name = "annual_price", nullable = false)
    @Builder.Default
    private BigDecimal annualPrice = BigDecimal.ZERO;

    @Column(name = "max_properties", nullable = false)
    @Builder.Default
    private Integer maxProperties = -1; // -1 = unlimited

    @Column(name = "max_contracts_per_month", nullable = false)
    @Builder.Default
    private Integer maxContractsPerMonth = -1; // -1 = unlimited

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> features;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
