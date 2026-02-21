package com.howners.gestion.repository;

import com.howners.gestion.domain.signature.Signature;
import com.howners.gestion.domain.signature.SignatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, UUID> {

    List<Signature> findByContractId(UUID contractId);

    List<Signature> findBySignerId(UUID signerId);

    List<Signature> findBySignerIdAndStatus(UUID signerId, SignatureStatus status);

    Optional<Signature> findByProviderSignatureId(String providerSignatureId);
}
