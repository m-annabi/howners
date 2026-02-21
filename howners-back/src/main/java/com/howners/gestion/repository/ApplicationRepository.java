package com.howners.gestion.repository;

import com.howners.gestion.domain.application.Application;
import com.howners.gestion.domain.application.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByListingIdOrderByCreatedAtDesc(UUID listingId);

    List<Application> findByApplicantIdOrderByCreatedAtDesc(UUID applicantId);

    @Query("SELECT a FROM Application a WHERE a.listing.property.owner.id = :ownerId ORDER BY a.createdAt DESC")
    List<Application> findByOwnerId(@Param("ownerId") UUID ownerId);

    boolean existsByListingIdAndApplicantId(UUID listingId, UUID applicantId);

    long countByListingIdAndStatus(UUID listingId, ApplicationStatus status);
}
