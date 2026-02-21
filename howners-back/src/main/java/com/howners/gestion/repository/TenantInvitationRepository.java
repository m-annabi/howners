package com.howners.gestion.repository;

import com.howners.gestion.domain.search.TenantInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantInvitationRepository extends JpaRepository<TenantInvitation, UUID> {

    List<TenantInvitation> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<TenantInvitation> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    boolean existsByListingIdAndTenantId(UUID listingId, UUID tenantId);
}
