package com.howners.gestion.repository;

import com.howners.gestion.domain.contract.ContractTemplate;
import com.howners.gestion.domain.rental.RentalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, UUID> {

    List<ContractTemplate> findByIsActiveTrue();

    List<ContractTemplate> findByRentalTypeAndIsActiveTrue(RentalType rentalType);

    Optional<ContractTemplate> findByRentalTypeAndIsDefaultTrue(RentalType rentalType);

    List<ContractTemplate> findByCreatedById(UUID createdById);

    /**
     * Récupère tous les templates accessibles (créés par l'utilisateur + templates par défaut)
     * LEFT JOIN FETCH pour charger createdBy et éviter LazyInitializationException
     */
    @Query("SELECT DISTINCT t FROM ContractTemplate t " +
           "LEFT JOIN FETCH t.createdBy " +
           "WHERE (t.createdBy.id = :userId OR t.isDefault = true) " +
           "AND t.isActive = true " +
           "AND (:rentalType IS NULL OR t.rentalType = :rentalType) " +
           "ORDER BY t.isDefault DESC, t.createdAt DESC")
    List<ContractTemplate> findAccessibleTemplates(@Param("userId") UUID userId,
                                                    @Param("rentalType") RentalType rentalType);

    /**
     * Récupère un template par ID avec createdBy chargé (fetch join)
     */
    @Query("SELECT t FROM ContractTemplate t " +
           "LEFT JOIN FETCH t.createdBy " +
           "WHERE t.id = :id")
    Optional<ContractTemplate> findByIdWithCreatedBy(@Param("id") UUID id);

    /**
     * Vérifie si l'utilisateur est propriétaire du template
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
           "FROM ContractTemplate t WHERE t.id = :templateId AND t.createdBy.id = :userId")
    boolean isOwner(@Param("templateId") UUID templateId, @Param("userId") UUID userId);
}
