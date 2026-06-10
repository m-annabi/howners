package com.howners.gestion.repository;

import com.howners.gestion.domain.affiliate.AffiliateCategory;
import com.howners.gestion.domain.affiliate.AffiliatePartner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AffiliatePartnerRepository extends JpaRepository<AffiliatePartner, UUID> {
    List<AffiliatePartner> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<AffiliatePartner> findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(AffiliateCategory category);
    Optional<AffiliatePartner> findBySlug(String slug);
}
