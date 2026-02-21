package com.howners.gestion.repository;

import com.howners.gestion.domain.security.EncryptionKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EncryptionKeyRepository extends JpaRepository<EncryptionKey, UUID> {

    Optional<EncryptionKey> findByKeyAlias(String keyAlias);

    Optional<EncryptionKey> findFirstByActiveTrueOrderByCreatedAtDesc();
}
