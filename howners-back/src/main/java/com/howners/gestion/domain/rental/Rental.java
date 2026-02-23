package com.howners.gestion.domain.rental;

import com.howners.gestion.domain.application.Application;
import com.howners.gestion.domain.property.Property;
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
@Table(name = "rentals")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private User tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "rental_type", nullable = false)
    private RentalType rentalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RentalStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "monthly_rent", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyRent;

    @Column(length = 3)
    private String currency = "EUR";

    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal charges;

    @Column(name = "payment_day")
    private Integer paymentDay;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", unique = true)
    private Application application;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
