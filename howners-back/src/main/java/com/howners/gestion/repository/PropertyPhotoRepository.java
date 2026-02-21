package com.howners.gestion.repository;

import com.howners.gestion.domain.photo.PropertyPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyPhotoRepository extends JpaRepository<PropertyPhoto, UUID> {

    List<PropertyPhoto> findByPropertyIdOrderByDisplayOrderAsc(UUID propertyId);

    Optional<PropertyPhoto> findByPropertyIdAndIsPrimaryTrue(UUID propertyId);

    long countByPropertyId(UUID propertyId);

    @Query("SELECT CASE WHEN COUNT(p) >= 5 THEN true ELSE false END FROM PropertyPhoto p WHERE p.property.id = :propertyId")
    boolean hasReachedPhotoLimit(@Param("propertyId") UUID propertyId);

    @Modifying
    @Query("UPDATE PropertyPhoto p SET p.isPrimary = false WHERE p.property.id = :propertyId")
    void clearPrimaryFlagForProperty(@Param("propertyId") UUID propertyId);

    Optional<PropertyPhoto> findByFileKey(String fileKey);
}
