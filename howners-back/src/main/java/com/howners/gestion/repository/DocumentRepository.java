package com.howners.gestion.repository;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByRentalId(UUID rentalId);

    List<Document> findByPropertyId(UUID propertyId);

    List<Document> findByUploaderId(UUID uploaderId);

    List<Document> findByDocumentType(DocumentType documentType);

    List<Document> findByRentalIdAndDocumentType(UUID rentalId, DocumentType documentType);

    @Query("SELECT d FROM Document d WHERE d.isArchived = false AND d.retentionEndDate IS NOT NULL AND d.retentionEndDate < :date AND d.legalHold = false")
    List<Document> findExpiredDocuments(@Param("date") LocalDate date);

    List<Document> findByIsArchivedTrue();

    List<Document> findByApplicationId(UUID applicationId);
}
