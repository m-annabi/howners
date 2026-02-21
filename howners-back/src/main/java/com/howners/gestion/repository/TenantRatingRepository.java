package com.howners.gestion.repository;

import com.howners.gestion.domain.rating.TenantRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantRatingRepository extends JpaRepository<TenantRating, UUID> {

    List<TenantRating> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<TenantRating> findByRaterIdOrderByCreatedAtDesc(UUID raterId);

    List<TenantRating> findByRentalIdOrderByCreatedAtDesc(UUID rentalId);

    @Query("SELECT AVG(r.paymentRating), AVG(r.propertyRespectRating), AVG(r.communicationRating), AVG(r.overallRating), COUNT(r) " +
            "FROM TenantRating r WHERE r.tenant.id = :tenantId")
    Object[] getAverageRatingsByTenantId(UUID tenantId);

    @Query("SELECT r FROM TenantRating r WHERE r.rental.property.owner.id = :ownerId ORDER BY r.createdAt DESC")
    List<TenantRating> findByPropertyOwnerId(UUID ownerId);
}
