package com.howners.gestion.repository;

import com.howners.gestion.domain.invoice.Invoice;
import com.howners.gestion.domain.invoice.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByRentalId(UUID rentalId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByStatus(InvoiceStatus status);

    @Query("SELECT i FROM Invoice i WHERE i.rental.property.owner.id = :ownerId ORDER BY i.issueDate DESC")
    List<Invoice> findByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT i FROM Invoice i WHERE i.status = 'ISSUED' AND i.dueDate < :now")
    List<Invoice> findOverdueInvoices(@Param("now") LocalDate now);
}
