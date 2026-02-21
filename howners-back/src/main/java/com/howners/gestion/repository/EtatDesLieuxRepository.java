package com.howners.gestion.repository;

import com.howners.gestion.domain.inventory.EtatDesLieux;
import com.howners.gestion.domain.inventory.EtatDesLieuxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EtatDesLieuxRepository extends JpaRepository<EtatDesLieux, UUID> {

    List<EtatDesLieux> findByRentalIdOrderByInspectionDateDesc(UUID rentalId);

    Optional<EtatDesLieux> findByRentalIdAndType(UUID rentalId, EtatDesLieuxType type);

    @Query("SELECT e FROM EtatDesLieux e WHERE e.rental.property.owner.id = :ownerId ORDER BY e.inspectionDate DESC")
    List<EtatDesLieux> findByOwnerId(@Param("ownerId") UUID ownerId);
}
