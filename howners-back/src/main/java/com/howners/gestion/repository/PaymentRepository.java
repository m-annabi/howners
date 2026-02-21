package com.howners.gestion.repository;

import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByRentalId(UUID rentalId);

    List<Payment> findByPayerId(UUID payerId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByRentalIdAndStatus(UUID rentalId, PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.rental.property.owner.id = :ownerId ORDER BY p.createdAt DESC")
    List<Payment> findByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.dueDate < :now")
    List<Payment> findOverduePayments(@Param("now") LocalDate now);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.rental.property.owner.id = :ownerId AND p.status = 'PAID' AND p.paidAt >= :from AND p.paidAt < :to")
    BigDecimal sumPaidAmountByOwnerAndPeriod(@Param("ownerId") UUID ownerId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.payer.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.payer.id = :tenantId AND p.status = 'PAID' AND (p.dueDate IS NULL OR p.paidAt <= CAST(p.dueDate AS timestamp))")
    long countOnTimePaymentsByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.payer.id = :tenantId AND (p.status = 'LATE' OR (p.status = 'PAID' AND p.dueDate IS NOT NULL AND p.paidAt > CAST(p.dueDate AS timestamp)))")
    long countLatePaymentsByTenantId(@Param("tenantId") UUID tenantId);
}
