package com.howners.gestion.repository;

import com.howners.gestion.domain.subscription.UsageTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsageTrackingRepository extends JpaRepository<UsageTracking, UUID> {

    Optional<UsageTracking> findByUserIdAndMetricAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
            UUID userId, String metric, LocalDateTime date1, LocalDateTime date2);
}
