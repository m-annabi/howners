package com.howners.gestion.repository;

import com.howners.gestion.domain.rental.ChargeRegularisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChargeRegularisationRepository extends JpaRepository<ChargeRegularisation, UUID> {

    List<ChargeRegularisation> findByRentalIdOrderByAnneeDesc(UUID rentalId);

    Optional<ChargeRegularisation> findByRentalIdAndAnnee(UUID rentalId, Integer annee);
}
