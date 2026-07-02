package com.howners.gestion.repository;

import com.howners.gestion.domain.accounting.AmortizableAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AmortizableAssetRepository extends JpaRepository<AmortizableAsset, UUID> {
    List<AmortizableAsset> findByActivityId(UUID activityId);
}
