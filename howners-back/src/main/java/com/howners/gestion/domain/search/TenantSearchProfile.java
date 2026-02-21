package com.howners.gestion.domain.search;

import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_search_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSearchProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private User tenant;

    @Column(name = "desired_city")
    private String desiredCity;

    @Column(name = "desired_department", length = 10)
    private String desiredDepartment;

    @Column(name = "desired_postal_code", length = 10)
    private String desiredPostalCode;

    @Column(name = "budget_min", precision = 10, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 10, scale = 2)
    private BigDecimal budgetMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "desired_property_type", length = 30)
    private PropertyType desiredPropertyType;

    @Column(name = "min_surface", precision = 10, scale = 2)
    private BigDecimal minSurface;

    @Column(name = "min_bedrooms")
    private Integer minBedrooms;

    @Enumerated(EnumType.STRING)
    @Column(name = "furnished_preference", length = 20)
    @Builder.Default
    private FurnishedPreference furnishedPreference = FurnishedPreference.NO_PREFERENCE;

    @Column(name = "desired_move_in")
    private LocalDate desiredMoveIn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
