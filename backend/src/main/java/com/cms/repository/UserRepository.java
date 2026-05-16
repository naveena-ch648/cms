package com.cms.repository;

import com.cms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUuid(String uuid);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmailAndOrganizationId(String email, Long organizationId);

    boolean existsByEmailAndOrganizationId(String email, Long organizationId);

    Page<User> findByOrganizationId(Long organizationId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.organization.id = :orgId AND " +
            "(LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchByOrganizationId(@Param("orgId") Long orgId, @Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.organization.id = :orgId AND u.status = :status")
    Page<User> findByOrganizationIdAndStatus(@Param("orgId") Long orgId, @Param("status") User.UserStatus status, Pageable pageable);

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndStatus(Long organizationId, User.UserStatus status);

    @Query("SELECT COUNT(DISTINCT u.id) FROM User u WHERE u.organization.id = :orgId AND u.lastLoginAt >= :since")
    long countActiveUsersSince(@Param("orgId") Long orgId, @Param("since") Instant since);
}
