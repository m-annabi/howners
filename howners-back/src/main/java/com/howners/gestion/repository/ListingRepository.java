package com.howners.gestion.repository;

import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.listing.ListingStatus;
import com.howners.gestion.domain.property.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {

    List<Listing> findByStatusOrderByPublishedAtDesc(ListingStatus status);

    @Query("SELECT l FROM Listing l WHERE l.property.owner.id = :ownerId ORDER BY l.createdAt DESC")
    List<Listing> findByOwnerId(@Param("ownerId") UUID ownerId);

    List<Listing> findByPropertyId(UUID propertyId);

    @Query("SELECT l FROM Listing l WHERE l.status = 'PUBLISHED' AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(l.property.city) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(l.property.postalCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(l.property.department) LIKE LOWER(CONCAT('%', :search, '%')))" +
           " ORDER BY l.publishedAt DESC")
    List<Listing> searchPublished(@Param("search") String search);

    @Query("SELECT l FROM Listing l WHERE l.status = 'PUBLISHED' " +
           "AND (:search = '' OR " +
           "  LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(l.property.city) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(l.property.postalCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(l.property.department) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:city = '' OR LOWER(l.property.city) = LOWER(:city)) " +
           "AND (:department = '' OR LOWER(l.property.department) = LOWER(:department)) " +
           "AND (:postalCode = '' OR l.property.postalCode LIKE CONCAT(:postalCode, '%')) " +
           "ORDER BY l.publishedAt DESC")
    List<Listing> searchPublishedWithFilters(
            @Param("search") String search,
            @Param("city") String city,
            @Param("department") String department,
            @Param("postalCode") String postalCode);

    @Query("SELECT l FROM Listing l WHERE l.status = 'PUBLISHED' " +
           "AND (:search = '' OR " +
           "  LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(l.property.city) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(l.property.postalCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(l.property.department) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:city = '' OR LOWER(l.property.city) = LOWER(:city)) " +
           "AND (:department = '' OR LOWER(l.property.department) = LOWER(:department)) " +
           "AND (:postalCode = '' OR l.property.postalCode LIKE CONCAT(:postalCode, '%')) " +
           "AND (:priceMin IS NULL OR l.pricePerMonth >= :priceMin) " +
           "AND (:priceMax IS NULL OR l.pricePerMonth <= :priceMax) " +
           "AND (:propertyType IS NULL OR l.property.propertyType = :propertyType) " +
           "AND (:minSurface IS NULL OR l.property.surfaceArea >= :minSurface) " +
           "AND (:minBedrooms IS NULL OR l.property.bedrooms >= :minBedrooms) " +
           "AND (:furnished IS NULL OR l.property.isFurnished = :furnished) " +
           "AND (:availableFrom IS NULL OR l.availableFrom IS NULL OR l.availableFrom <= :availableFrom) " +
           "ORDER BY l.publishedAt DESC")
    List<Listing> searchPublishedAdvanced(
            @Param("search") String search,
            @Param("city") String city,
            @Param("department") String department,
            @Param("postalCode") String postalCode,
            @Param("priceMin") BigDecimal priceMin,
            @Param("priceMax") BigDecimal priceMax,
            @Param("propertyType") PropertyType propertyType,
            @Param("minSurface") BigDecimal minSurface,
            @Param("minBedrooms") Integer minBedrooms,
            @Param("furnished") Boolean furnished,
            @Param("availableFrom") LocalDate availableFrom);
}
