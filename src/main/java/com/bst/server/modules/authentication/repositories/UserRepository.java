package com.bst.server.modules.authentication.repositories;

import com.bst.server.modules.authentication.data.entities.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID>,
        JpaSpecificationExecutor<Users> {

    // ─────────────────────────────────────────────
    // Basic lookups
    // ─────────────────────────────────────────────

    Optional<Users> findByIdAndDeletedFalse(UUID id);

    Optional<Users> findByEmailAndDeletedFalse(String email);

    boolean existsByEmailAndDeletedFalse(String email);

    // =========================================================
    // SECURITY FETCH
    // =========================================================

    @EntityGraph(attributePaths = {
            "roles",
            "roles.permissions"
    })
    @Query("""
        SELECT u
        FROM Users u
        WHERE u.email = :email
        AND u.deleted = false
    """)
    Optional<Users> findSecurityUserByEmail(
            @Param("email") String email
    );

    // ─────────────────────────────────────────────
    // Auth / Security
    // ─────────────────────────────────────────────

    /**
     * Used during JWT refresh — validates token hash before issuing a new pair.
     */
    @Query("""
        SELECT u FROM Users u
        WHERE u.refreshTokenHash = :tokenHash
          AND u.refreshTokenExpiresAt > :now
          AND u.enabled = true
        """)
    Optional<Users> findByValidRefreshToken(
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now
    );

    /**
     * Fetch user with roles + permissions eagerly in a single query.
     * Avoids N+1 when performing permission checks on login.
     */
    @Query("""
        SELECT DISTINCT u FROM Users u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions
        WHERE u.id = :id
        """)
    Optional<Users> findByIdWithRolesAndPermissions(@Param("id") UUID id);

    // ─────────────────────────────────────────────
    // Account lock management
    // ─────────────────────────────────────────────

    /**
     * Atomically increments failed login attempts.
     * Call this on every failed login attempt.
     */
    @Modifying
    @Query("""
        UPDATE Users u
        SET u.failedLoginAttempts = u.failedLoginAttempts + 1,
            u.updatedAt = :now
        WHERE u.id = :id
        """)
    void incrementFailedLoginAttempts(@Param("id") UUID id, @Param("now") LocalDateTime now);

    /**
     * Locks account until the given timestamp and resets attempt counter.
     */
    @Modifying
    @Query("""
        UPDATE Users u
        SET u.lockedUntil           = :lockedUntil,
            u.failedLoginAttempts   = 0,
            u.updatedAt             = :now
        WHERE u.id = :id
        """)
    void lockAccount(
            @Param("id") UUID id,
            @Param("lockedUntil") LocalDateTime lockedUntil,
            @Param("now") LocalDateTime now
    );

    /**
     * Unlocks account and clears failed attempt counter on successful login.
     */
    @Modifying
    @Query("""
        UPDATE Users u
        SET u.lockedUntil           = NULL,
            u.failedLoginAttempts   = 0,
            u.lastLoginAt           = :now,
            u.updatedAt             = :now
        WHERE u.id = :id
        """)
    void unlockAndRecordLogin(@Param("id") UUID id, @Param("now") LocalDateTime now);

    // ─────────────────────────────────────────────
    // Refresh token lifecycle
    // ─────────────────────────────────────────────

    @Modifying
    @Query("""
        UPDATE Users u
        SET u.refreshTokenHash       = :tokenHash,
            u.refreshTokenExpiresAt  = :expiresAt,
            u.updatedAt              = :now
        WHERE u.id = :id
        """)
    void saveRefreshToken(
            @Param("id") UUID id,
            @Param("tokenHash") String tokenHash,
            @Param("expiresAt") LocalDateTime expiresAt,
            @Param("now") LocalDateTime now
    );

    /**
     * Invalidates the refresh token on logout or token rotation.
     */
    @Modifying
    @Query("""
        UPDATE Users u
        SET u.refreshTokenHash      = NULL,
            u.refreshTokenExpiresAt = NULL,
            u.updatedAt             = :now
        WHERE u.id = :id
        """)
    void revokeRefreshToken(@Param("id") UUID id, @Param("now") LocalDateTime now);

    // ─────────────────────────────────────────────
    // Password management
    // ─────────────────────────────────────────────

    @Modifying
    @Query("""
        UPDATE Users u
        SET u.passwordHash        = :newHash,
            u.passwordChangedAt   = :now,
            u.refreshTokenHash    = NULL,
            u.refreshTokenExpiresAt = NULL,
            u.updatedAt           = :now
        WHERE u.id = :id
        """)
    void changePassword(
            @Param("id") UUID id,
            @Param("newHash") String newHash,
            @Param("now") LocalDateTime now
    );

    // ─────────────────────────────────────────────
    // Admin — user listing & filtering
    // ─────────────────────────────────────────────

    /**
     * All users who hold a specific role — used for admin dashboards.
     */
    @Query("""
        SELECT DISTINCT u FROM Users u
        JOIN u.roles r
        WHERE r.name = :roleName
          AND u.enabled = :enabled
        """)
    List<Users> findAllByRoleNameAndStatus(
            @Param("roleName") String roleName,
            @Param("enabled") boolean enabled
    );

    /**
     * Users whose accounts are currently locked — for admin review.
     */
    @Query("""
        SELECT u FROM Users u
        WHERE u.lockedUntil IS NOT NULL
          AND u.lockedUntil > :now
        """)
    List<Users> findAllLockedAccounts(@Param("now") LocalDateTime now);

    /**
     * Users who have never logged in — useful for cleanup or onboarding reminders.
     */
    @Query("SELECT u FROM Users u WHERE u.lastLoginAt IS NULL AND u.enabled = true")
    List<Users> findNeverLoggedInUsers();

    /**
     * Users inactive (no login) since a given date — for audit or deactivation.
     */
    @Query("""
        SELECT u FROM Users u
        WHERE u.lastLoginAt < :since
          AND u.enabled = true
        """)
    List<Users> findInactiveUsersSince(@Param("since") LocalDateTime since);

    // ─────────────────────────────────────────────
    // Soft deactivation
    // ─────────────────────────────────────────────

    @Modifying
    @Query("""
        UPDATE Users u
        SET u.enabled              = false,
            u.refreshTokenHash      = NULL,
            u.refreshTokenExpiresAt = NULL,
            u.updatedAt             = :now
        WHERE u.id = :id
        """)
    void deactivateUser(@Param("id") UUID id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Users u SET u.enabled = true, u.updatedAt = :now WHERE u.id = :id")
    void reactivateUser(@Param("id") UUID id, @Param("now") LocalDateTime now);

    // ─────────────────────────────────────────────
    // Phone / Email verification
    // ─────────────────────────────────────────────

    @Modifying
    @Query("UPDATE Users u SET u.isEmailVerified = true, u.updatedAt = :now WHERE u.id = :id")
    void markEmailVerified(@Param("id") UUID id, @Param("now") LocalDateTime now);

    // ─────────────────────────────────────────────
    // Reporting / counts
    // ─────────────────────────────────────────────

    long countByEnabled(boolean enabled);

    @Query("""
        SELECT COUNT(DISTINCT u) FROM Users u
        JOIN u.roles r
        WHERE r.name = :roleName
        """)
    long countUsersByRole(@Param("roleName") String roleName);


    // =========================================================
    // SEARCH
    // =========================================================

    @Query("""
        SELECT u
        FROM Users u
        WHERE u.deleted = false
        AND (
            LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR u.phone LIKE CONCAT('%', :keyword, '%')
        )
    """)
    Page<Users> search(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // =========================================================
    // ROLE FILTERS
    // =========================================================

    @Query("""
        SELECT DISTINCT u
        FROM Users u
        JOIN u.roles r
        WHERE r.name = :roleName
        AND u.deleted = false
    """)
    Page<Users> findByRole(
            @Param("roleName") String roleName,
            Pageable pageable
    );

    // =========================================================
    // LOCKED USERS
    // =========================================================

    @Query("""
        SELECT u
        FROM Users u
        WHERE u.lockedUntil IS NOT NULL
        AND u.lockedUntil > :now
        AND u.deleted = false
    """)
    List<Users> findLockedUsers(
            @Param("now") LocalDateTime now
    );

    // =========================================================
    // LOGIN TRACKING
    // =========================================================

    @Modifying
    @Query("""
        UPDATE Users u
        SET u.failedLoginAttempts = u.failedLoginAttempts + 1
        WHERE u.id = :id
    """)
    void incrementFailedAttempts(@Param("id") UUID id);

    @Modifying
    @Query("""
        UPDATE Users u
        SET u.failedLoginAttempts = 0,
            u.lockedUntil = NULL,
            u.lastLoginAt = :now
        WHERE u.id = :id
    """)
    void markSuccessfulLogin(
            @Param("id") UUID id,
            @Param("now") LocalDateTime now
    );

    // =========================================================
    // ACCOUNT LOCK
    // =========================================================

    @Modifying
    @Query("""
        UPDATE Users u
        SET u.lockedUntil = :lockedUntil
        WHERE u.id = :id
    """)
    void lockAccount(
            @Param("id") UUID id,
            @Param("lockedUntil") LocalDateTime lockedUntil
    );

    // =========================================================
    // SOFT DELETE
    // =========================================================

    @Modifying
    @Query("""
        UPDATE Users u
        SET u.deleted = true,
            u.enabled = false
        WHERE u.id = :id
    """)
    void softDelete(@Param("id") UUID id);

    // =========================================================
    // STATUS
    // =========================================================

    @Modifying
    @Query("""
        UPDATE Users u
        SET u.enabled = :status
        WHERE u.id = :id
    """)
    void updateStatus(
            @Param("id") UUID id,
            @Param("status") Boolean status
    );

    // =========================================================
    // COUNTS
    // =========================================================

    long countByDeletedFalse();

    long countByEnabledTrueAndDeletedFalse();

    // =========================================================
    // AUDIT
    // =========================================================

    @Query("""
        SELECT u
        FROM Users u
        WHERE u.lastLoginAt < :date
        AND u.deleted = false
    """)
    List<Users> findInactiveUsers(
            @Param("date") LocalDateTime date
    );
}
