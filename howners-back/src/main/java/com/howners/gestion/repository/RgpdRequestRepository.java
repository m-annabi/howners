package com.howners.gestion.repository;

import com.howners.gestion.domain.rgpd.RgpdRequest;
import com.howners.gestion.domain.rgpd.RgpdRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RgpdRequestRepository extends JpaRepository<RgpdRequest, UUID> {

    List<RgpdRequest> findByUserIdOrderByRequestedAtDesc(UUID userId);

    /** Demandes encore ouvertes au-delà du délai légal (pour supervision). */
    List<RgpdRequest> findByStatusAndRequestedAtBefore(RgpdRequestStatus status, LocalDateTime seuil);
}
