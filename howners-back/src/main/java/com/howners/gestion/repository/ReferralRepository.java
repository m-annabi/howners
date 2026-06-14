package com.howners.gestion.repository;

import com.howners.gestion.domain.referral.Referral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralRepository extends JpaRepository<Referral, UUID> {
    List<Referral> findByReferrerIdOrderByCreatedAtDesc(UUID referrerId);
    long countByReferrerId(UUID referrerId);
    Optional<Referral> findByRefereeId(UUID refereeId);
    boolean existsByRefereeId(UUID refereeId);
}
