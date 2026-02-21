package com.howners.gestion.domain.property;

import com.howners.gestion.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "properties")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false)
    private PropertyType propertyType;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    private String department;

    @Column(length = 2)
    private String country = "FR";

    @Column(name = "surface_area", precision = 10, scale = 2)
    private BigDecimal surfaceArea;

    private Integer bedrooms;

    private Integer bathrooms;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Charges et taxes
    @Column(name = "condo_fees", precision = 10, scale = 2)
    private BigDecimal condoFees; // Charges de copropriété mensuelles

    @Column(name = "property_tax", precision = 10, scale = 2)
    private BigDecimal propertyTax; // Taxe foncière annuelle

    @Column(name = "business_tax", precision = 10, scale = 2)
    private BigDecimal businessTax; // Taxe CFE annuelle

    @Column(name = "home_insurance", precision = 10, scale = 2)
    private BigDecimal homeInsurance; // Assurance habitation annuelle

    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice; // Prix d'achat pour calcul de rentabilité

    // Informations techniques
    @Column(name = "dpe_rating", length = 1)
    private String dpeRating; // Diagnostic Performance Énergétique (A-G)

    @Column(name = "ges_rating", length = 1)
    private String gesRating; // Gaz à Effet de Serre (A-G)

    @Column(name = "construction_year")
    private Integer constructionYear;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "total_floors")
    private Integer totalFloors;

    @Enumerated(EnumType.STRING)
    @Column(name = "heating_type", length = 30)
    private HeatingType heatingType;

    @Column(name = "has_parking")
    private Boolean hasParking;

    @Column(name = "has_elevator")
    private Boolean hasElevator;

    @Column(name = "is_furnished")
    private Boolean isFurnished;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_condition", length = 20)
    private PropertyCondition propertyCondition;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
