package com.howners.gestion.repository;

import com.howners.gestion.domain.contract.ContractSignatureRequest;
import com.howners.gestion.domain.contract.SignatureRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractSignatureRequestRepository extends JpaRepository<ContractSignatureRequest, UUID> {

    /**
     * Trouve une demande de signature par son token d'accès
     */
    Optional<ContractSignatureRequest> findByAccessToken(String accessToken);

    /**
     * Trouve une demande de signature par l'ID du contrat
     */
    @Query("SELECT csr FROM ContractSignatureRequest csr " +
           "LEFT JOIN FETCH csr.contract c " +
           "LEFT JOIN FETCH csr.signer s " +
           "WHERE c.id = :contractId")
    Optional<ContractSignatureRequest> findByContractId(@Param("contractId") UUID contractId);

    /**
     * Trouve une demande de signature par l'ID de l'enveloppe du fournisseur
     */
    Optional<ContractSignatureRequest> findByProviderEnvelopeId(String providerEnvelopeId);

    /**
     * Trouve toutes les demandes de signature par statut
     */
    List<ContractSignatureRequest> findByStatus(SignatureRequestStatus status);

    /**
     * Trouve toutes les demandes de signature d'un utilisateur
     */
    @Query("SELECT csr FROM ContractSignatureRequest csr " +
           "LEFT JOIN FETCH csr.contract c " +
           "LEFT JOIN FETCH csr.signer s " +
           "WHERE s.id = :signerId " +
           "ORDER BY csr.createdAt DESC")
    List<ContractSignatureRequest> findBySignerId(@Param("signerId") UUID signerId);

    /**
     * Trouve une demande de signature avec toutes ses relations chargées
     */
    @Query("SELECT csr FROM ContractSignatureRequest csr " +
           "LEFT JOIN FETCH csr.contract c " +
           "LEFT JOIN FETCH c.rental r " +
           "LEFT JOIN FETCH r.property p " +
           "LEFT JOIN FETCH csr.signer s " +
           "WHERE csr.id = :id")
    Optional<ContractSignatureRequest> findByIdWithDetails(@Param("id") UUID id);

    /**
     * Trouve une demande de signature par token avec toutes ses relations
     */
    @Query("SELECT csr FROM ContractSignatureRequest csr " +
           "LEFT JOIN FETCH csr.contract c " +
           "LEFT JOIN FETCH c.rental r " +
           "LEFT JOIN FETCH r.property p " +
           "LEFT JOIN FETCH r.tenant t " +
           "LEFT JOIN FETCH p.owner o " +
           "LEFT JOIN FETCH csr.signer s " +
           "WHERE csr.accessToken = :token")
    Optional<ContractSignatureRequest> findByAccessTokenWithDetails(@Param("token") String token);

    /**
     * Compte les demandes de signature par statut pour un contrat donné
     */
    @Query("SELECT COUNT(csr) FROM ContractSignatureRequest csr " +
           "WHERE csr.contract.id = :contractId AND csr.status = :status")
    long countByContractIdAndStatus(@Param("contractId") UUID contractId,
                                    @Param("status") SignatureRequestStatus status);

    /**
     * Vérifie si un contrat a déjà une demande de signature en cours
     */
    @Query("SELECT CASE WHEN COUNT(csr) > 0 THEN true ELSE false END " +
           "FROM ContractSignatureRequest csr " +
           "WHERE csr.contract.id = :contractId " +
           "AND csr.status IN ('PENDING', 'SENT', 'VIEWED')")
    boolean hasActiveSignatureRequest(@Param("contractId") UUID contractId);

    /**
     * Trouve les demandes de signature envoyées avant une date donnée avec moins de N rappels
     */
    @Query("SELECT csr FROM ContractSignatureRequest csr " +
           "LEFT JOIN FETCH csr.contract c " +
           "LEFT JOIN FETCH c.rental r " +
           "LEFT JOIN FETCH r.property p " +
           "LEFT JOIN FETCH r.tenant t " +
           "LEFT JOIN FETCH p.owner o " +
           "LEFT JOIN FETCH csr.signer s " +
           "WHERE csr.status = 'SENT' " +
           "AND csr.sentAt < :sentBefore " +
           "AND csr.reminderCount < :maxReminders")
    List<ContractSignatureRequest> findSentRequestsNeedingReminder(
            @Param("sentBefore") LocalDateTime sentBefore,
            @Param("maxReminders") int maxReminders);

    /**
     * Trouve toutes les demandes de signature pour le propriétaire courant
     */
    @Query("SELECT csr FROM ContractSignatureRequest csr " +
           "LEFT JOIN FETCH csr.contract c " +
           "LEFT JOIN FETCH c.rental r " +
           "LEFT JOIN FETCH r.property p " +
           "LEFT JOIN FETCH csr.signer s " +
           "WHERE p.owner.id = :ownerId " +
           "ORDER BY csr.createdAt DESC")
    List<ContractSignatureRequest> findByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Compte les demandes par statut pour un propriétaire
     */
    @Query("SELECT csr.status, COUNT(csr) FROM ContractSignatureRequest csr " +
           "WHERE csr.contract.rental.property.owner.id = :ownerId " +
           "GROUP BY csr.status")
    List<Object[]> countByStatusForOwner(@Param("ownerId") UUID ownerId);
}
