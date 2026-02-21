package com.howners.gestion.domain.listing;

import com.howners.gestion.domain.property.Property;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "listings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_per_night")
    private BigDecimal pricePerNight;

    @Column(name = "price_per_month")
    private BigDecimal pricePerMonth;

    @Column(length = 3)
    @Builder.Default
    private String currency = "EUR";

    @Column(name = "min_stay")
    private Integer minStay;

    @Column(name = "max_stay")
    private Integer maxStay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ListingStatus status = ListingStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String amenities;

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String requirements;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ListingPhoto> photos = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
