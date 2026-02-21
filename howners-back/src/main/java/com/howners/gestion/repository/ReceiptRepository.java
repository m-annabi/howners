package com.howners.gestion.repository;

import com.howners.gestion.domain.receipt.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    List<Receipt> findByRentalId(UUID rentalId);

    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    Optional<Receipt> findByPaymentId(UUID paymentId);

    @Query("SELECT r FROM Receipt r WHERE r.rental.property.owner.id = :ownerId ORDER BY r.periodEnd DESC")
    List<Receipt> findByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT r FROM Receipt r WHERE r.rental.tenant.id = :tenantId ORDER BY r.periodEnd DESC")
    List<Receipt> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM Receipt r WHERE r.rental.id = :rentalId AND r.periodStart = :start AND r.periodEnd = :end")
    Optional<Receipt> findByRentalAndPeriod(@Param("rentalId") UUID rentalId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
