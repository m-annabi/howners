package com.howners.gestion.repository;

import com.howners.gestion.domain.accounting.FiscalActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FiscalActivityRepository extends JpaRepository<FiscalActivity, UUID> {
    Optional<FiscalActivity> findByOwnerId(UUID ownerId);
}
