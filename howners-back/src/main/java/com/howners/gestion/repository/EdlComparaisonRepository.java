package com.howners.gestion.repository;

import com.howners.gestion.domain.inventory.EdlComparaison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EdlComparaisonRepository extends JpaRepository<EdlComparaison, UUID> {

    Optional<EdlComparaison> findByRentalId(UUID rentalId);
}
