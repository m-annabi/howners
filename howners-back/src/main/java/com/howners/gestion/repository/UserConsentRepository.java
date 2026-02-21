package com.howners.gestion.repository;

import com.howners.gestion.domain.audit.ConsentType;
import com.howners.gestion.domain.audit.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {

    List<UserConsent> findByUserId(UUID userId);

    Optional<UserConsent> findByUserIdAndConsentType(UUID userId, ConsentType consentType);
}
