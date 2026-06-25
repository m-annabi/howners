package com.howners.gestion.repository;

import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    Optional<User> findByReferralCode(String referralCode);

    boolean existsByReferralCode(String referralCode);

    Optional<User> findByStripeConnectAccountId(String stripeConnectAccountId);

    @Query("SELECT DISTINCT r.tenant FROM Rental r WHERE r.property.owner.id = :ownerId AND r.tenant IS NOT NULL")
    List<User> findTenantsByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT u FROM User u WHERE u.role = 'OWNER' AND u.enabled = true AND u.createdAt >= :from AND u.createdAt < :to")
    List<User> findOwnersCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
