package com.howners.gestion.repository;

import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RentalRepository extends JpaRepository<Rental, UUID> {

    List<Rental> findByPropertyId(UUID propertyId);

    List<Rental> findByTenantId(UUID tenantId);

    Page<Rental> findByTenantId(UUID tenantId, Pageable pageable);

    List<Rental> findByStatus(RentalStatus status);

    @Query("SELECT r FROM Rental r WHERE r.property.owner.id = :ownerId")
    List<Rental> findByOwnerId(UUID ownerId);

    @Query("SELECT r FROM Rental r WHERE r.property.owner.id = :ownerId")
    Page<Rental> findByOwnerId(@Param("ownerId") UUID ownerId, Pageable pageable);

    boolean existsByApplicationId(UUID applicationId);

    @Query("SELECT r FROM Rental r WHERE r.endDate = :targetDate AND r.status = 'ACTIVE'")
    List<Rental> findActiveRentalsEndingOn(@Param("targetDate") LocalDate targetDate);

    @Query("SELECT r FROM Rental r WHERE r.status = 'ACTIVE' AND r.assuranceExpiration IS NOT NULL AND r.assuranceExpiration <= :limite")
    List<Rental> findActiveWithAssuranceExpiringBefore(@Param("limite") LocalDate limite);
}
