package com.howners.gestion.repository;

import com.howners.gestion.domain.contract.ContractVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractVersionRepository extends JpaRepository<ContractVersion, UUID> {

    List<ContractVersion> findByContractIdOrderByVersionDesc(UUID contractId);

    Optional<ContractVersion> findByContractIdAndVersion(UUID contractId, Integer version);
}
