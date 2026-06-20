package com.howners.gestion.repository;

import com.howners.gestion.domain.rating.TenantRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantRatingRepository extends JpaRepository<TenantRating, UUID> {

    List<TenantRating> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    boolean existsByRaterIdAndRentalId(UUID raterId, UUID rentalId);
}
