package com.howners.gestion.repository;

import com.howners.gestion.domain.rental.RentRevision;
import com.howners.gestion.domain.rental.StatutRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RentRevisionRepository extends JpaRepository<RentRevision, UUID> {

    List<RentRevision> findByRentalIdOrderByCreatedAtDesc(UUID rentalId);

    boolean existsByRentalIdAndDateRevisionAfterAndStatutNot(
            UUID rentalId, LocalDate dateRevision, StatutRevision statut);

    @Query("SELECT r FROM RentRevision r WHERE r.rental.property.owner.id = :ownerId ORDER BY r.createdAt DESC")
    List<RentRevision> findByOwnerId(@Param("ownerId") UUID ownerId);
}
