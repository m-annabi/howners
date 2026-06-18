package com.howners.gestion.domain.rgpd;

import com.howners.gestion.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Trace une demande RGPD (export / effacement) pour respecter le délai légal de réponse
 * (1 mois, art. 12 RGPD). requested_at marque la réception, completed_at l'achèvement.
 */
@Entity
@Table(name = "rgpd_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RgpdRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private RgpdRequestType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RgpdRequestStatus status = RgpdRequestStatus.RECEIVED;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    public void markCompleted(String details) {
        this.status = RgpdRequestStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.details = details;
    }
}
