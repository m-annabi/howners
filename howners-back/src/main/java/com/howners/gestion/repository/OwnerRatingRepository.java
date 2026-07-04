package com.howners.gestion.repository;

import com.howners.gestion.domain.rating.OwnerRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OwnerRatingRepository extends JpaRepository<OwnerRating, UUID> {

    List<OwnerRating> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    boolean existsByRaterIdAndRentalId(UUID raterId, UUID rentalId);
}
