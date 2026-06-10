package com.howners.gestion.repository;

import com.howners.gestion.domain.widget.UserWidgetPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserWidgetPreferenceRepository extends JpaRepository<UserWidgetPreference, Long> {
    Optional<UserWidgetPreference> findByUserIdAndPage(UUID userId, String page);
}
