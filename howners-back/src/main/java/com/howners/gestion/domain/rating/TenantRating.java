package com.howners.gestion.domain.rating;

import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_ratings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantRating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rater_id", nullable = false)
    private User rater;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id")
    private Rental rental;

    @Column(name = "payment_rating", nullable = false)
    private Integer paymentRating;

    @Column(name = "property_respect_rating", nullable = false)
    private Integer propertyRespectRating;

    @Column(name = "communication_rating", nullable = false)
    private Integer communicationRating;

    @Column(name = "overall_rating", nullable = false)
    private Double overallRating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "rating_period", length = 50)
    private String ratingPeriod;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void calculateOverallRating() {
        this.overallRating = (paymentRating + propertyRespectRating + communicationRating) / 3.0;
    }
}
