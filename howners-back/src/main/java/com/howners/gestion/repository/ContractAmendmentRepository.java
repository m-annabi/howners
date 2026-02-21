package com.howners.gestion.repository;

import com.howners.gestion.domain.contract.ContractAmendment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContractAmendmentRepository extends JpaRepository<ContractAmendment, UUID> {

    List<ContractAmendment> findByContractIdOrderByAmendmentNumberDesc(UUID contractId);

    int countByContractId(UUID contractId);
}
