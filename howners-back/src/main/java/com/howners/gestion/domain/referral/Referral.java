package com.howners.gestion.domain.referral;

import com.howners.gestion.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "referrals", indexes = {
        @Index(name = "idx_referrals_referrer", columnList = "referrer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_id", nullable = false)
    private User referrer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_id", nullable = false, unique = true)
    private User referee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReferralStatus status = ReferralStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "referrer_rewarded_at")
    private LocalDateTime referrerRewardedAt;

    @Column(name = "referee_rewarded_at")
    private LocalDateTime refereeRewardedAt;
}
