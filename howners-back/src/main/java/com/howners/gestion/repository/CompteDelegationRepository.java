package com.howners.gestion.repository;

import com.howners.gestion.domain.user.CompteDelegation;
import com.howners.gestion.domain.user.StatutDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompteDelegationRepository extends JpaRepository<CompteDelegation, UUID> {

    List<CompteDelegation> findByAgenceIdOrderByCreatedAtDesc(UUID agenceUserId);

    List<CompteDelegation> findByDelegueIdAndStatutOrderByCreatedAtDesc(UUID delegueUserId, StatutDelegation statut);

    Optional<CompteDelegation> findByAgenceIdAndDelegueId(UUID agenceUserId, UUID delegueUserId);
}
