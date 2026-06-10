package com.howners.gestion.repository;

import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
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

    List<Rental> findByStatus(RentalStatus status);

    @Query("SELECT r FROM Rental r WHERE r.property.owner.id = :ownerId")
    List<Rental> findByOwnerId(UUID ownerId);

    boolean existsByApplicationId(UUID applicationId);

    @Query("SELECT r FROM Rental r WHERE r.endDate = :targetDate AND r.status = 'ACTIVE'")
    List<Rental> findActiveRentalsEndingOn(@Param("targetDate") LocalDate targetDate);
}
