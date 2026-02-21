package com.howners.gestion.repository;

import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.domain.search.TenantSearchProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantSearchProfileRepository extends JpaRepository<TenantSearchProfile, UUID> {

    Optional<TenantSearchProfile> findByTenantId(UUID tenantId);

    boolean existsByTenantId(UUID tenantId);

    @Query("SELECT p FROM TenantSearchProfile p WHERE p.isActive = true " +
           "AND (:city = '' OR LOWER(p.desiredCity) = LOWER(:city)) " +
           "AND (:department = '' OR LOWER(p.desiredDepartment) = LOWER(:department)) " +
           "AND (:postalCode = '' OR p.desiredPostalCode LIKE CONCAT(:postalCode, '%')) " +
           "AND (:budgetMin IS NULL OR p.budgetMax IS NULL OR p.budgetMax >= :budgetMin) " +
           "AND (:budgetMax IS NULL OR p.budgetMin IS NULL OR p.budgetMin <= :budgetMax) " +
           "AND (:propertyType IS NULL OR p.desiredPropertyType IS NULL OR p.desiredPropertyType = :propertyType) " +
           "ORDER BY p.updatedAt DESC")
    List<TenantSearchProfile> searchActiveProfiles(
            @Param("city") String city,
            @Param("department") String department,
            @Param("postalCode") String postalCode,
            @Param("budgetMin") BigDecimal budgetMin,
            @Param("budgetMax") BigDecimal budgetMax,
            @Param("propertyType") PropertyType propertyType);
}
