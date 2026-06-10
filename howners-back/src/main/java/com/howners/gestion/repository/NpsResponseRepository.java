package com.howners.gestion.repository;

import com.howners.gestion.domain.feedback.NpsResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NpsResponseRepository extends JpaRepository<NpsResponse, UUID> {
    boolean existsByUserId(UUID userId);
}
