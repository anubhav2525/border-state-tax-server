package com.bst.server.modules.authentication.repositories;

import com.bst.server.modules.authentication.data.entities.Permissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

import java.util.*;

@Repository
public interface PermissionRepository extends JpaRepository<Permissions, UUID> {

    // =========================================================
    // BASIC LOOKUPS
    // =========================================================

    Optional<Permissions> findByIdAndDeletedFalse(UUID id);

    Optional<Permissions> findByNameAndDeletedFalse(String name);

    Optional<Permissions> findByResourceAndActionAndDeletedFalse(
            String resource,
            String action
    );

    boolean existsByNameAndDeletedFalse(String name);

    boolean existsByResourceAndActionAndDeletedFalse(
            String resource,
            String action
    );

    List<Permissions> findAllDeletedFalse();

    // =========================================================
    // PAGINATION
    // =========================================================

    Page<Permissions> findAllByDeletedFalse(Pageable pageable);

    Page<Permissions> findAllByEnabledAndDeletedFalse(
            Boolean enabled,
            Pageable pageable
    );

    // =========================================================
    // SEARCHING
    // =========================================================

    @Query("""
                SELECT p FROM Permissions p
                WHERE p.deleted = false
                AND (
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(p.resource) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(p.action) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            """)
    Page<Permissions> search(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // =========================================================
    // ROLE BASED
    // =========================================================

    @Query("""
                SELECT DISTINCT p
                FROM Permissions p
                JOIN p.roles r
                WHERE r.id = :roleId
                AND p.deleted = false
                ORDER BY p.resource, p.action
            """)
    List<Permissions> findAllByRoleId(@Param("roleId") UUID roleId);

    @Query("""
                SELECT DISTINCT p.name
                FROM Permissions p
                JOIN p.roles r
                JOIN r.users u
                WHERE u.id = :userId
                AND u.enabled = true
                AND r.enabled = true
                AND p.enabled = true
                AND p.deleted = false
            """)
    Set<String> findPermissionNamesByUserId(@Param("userId") UUID userId);

    // =========================================================
    // BULK
    // =========================================================

    @Query("""
                SELECT p
                FROM Permissions p
                WHERE p.name IN :names
                AND p.deleted = false
            """)
    Set<Permissions> findAllByNames(@Param("names") Set<String> names);

    // =========================================================
    // ANALYTICS
    // =========================================================

    @Query("""
                SELECT p.resource, COUNT(p)
                FROM Permissions p
                WHERE p.deleted = false
                GROUP BY p.resource
            """)
    List<Object[]> countByResource();

    long countByDeletedFalse();

    long countByEnabledTrueAndDeletedFalse();

    // =========================================================
    // SOFT DELETE
    // =========================================================

    @Modifying
    @Query("""
                UPDATE Permissions p
                SET p.deleted = true,
                    p.enabled = false
                WHERE p.id = :id
            """)
    void softDelete(@Param("id") UUID id);

    // =========================================================
    // STATUS MANAGEMENT
    // =========================================================

    @Modifying
    @Query("""
                UPDATE Permissions p
                SET p.enabled = :status
                WHERE p.id = :id
            """)
    void updateStatus(
            @Param("id") UUID id,
            @Param("status") Boolean status
    );
}