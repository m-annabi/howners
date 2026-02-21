package com.howners.gestion.repository;

import com.howners.gestion.domain.listing.ListingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingPhotoRepository extends JpaRepository<ListingPhoto, UUID> {

    List<ListingPhoto> findByListingIdOrderByDisplayOrderAsc(UUID listingId);

    Optional<ListingPhoto> findByListingIdAndIsPrimaryTrue(UUID listingId);

    long countByListingId(UUID listingId);

    @Query("SELECT CASE WHEN COUNT(p) >= 5 THEN true ELSE false END FROM ListingPhoto p WHERE p.listing.id = :listingId")
    boolean hasReachedPhotoLimit(@Param("listingId") UUID listingId);

    @Modifying
    @Query("UPDATE ListingPhoto p SET p.isPrimary = false WHERE p.listing.id = :listingId")
    void clearPrimaryFlagForListing(@Param("listingId") UUID listingId);
}
