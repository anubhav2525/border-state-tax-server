package com.bst.server.modules.authentication.repositories;

import com.bst.server.modules.authentication.data.entities.Roles;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

import org.springframework.data.domain.*;

@Repository
public interface RoleRepository extends JpaRepository<Roles, UUID> {

    // =========================================================
    // BASIC
    // =========================================================

    Optional<Roles> findByIdAndDeletedFalse(UUID id);

    Optional<Roles> findByNameAndDeletedFalse(String name);

    boolean existsByNameAndDeletedFalse(String name);

    // =========================================================
    // FETCH OPTIMIZATION
    // =========================================================

    @EntityGraph(attributePaths = {
            "permissions"
    })
    @Query("""
                SELECT r
                FROM Roles r
                WHERE r.id = :id
                AND r.deleted = false
            """)
    Optional<Roles> findWithPermissions(@Param("id") UUID id);

    @EntityGraph(attributePaths = {
            "permissions",
            "users"
    })
    @Query("""
                SELECT DISTINCT r
                FROM Roles r
                WHERE r.deleted = false
            """)
    Page<Roles> findAllWithRelations(Pageable pageable);

    // =========================================================
    // SEARCH
    // =========================================================

    @Query("""
                SELECT r
                FROM Roles r
                WHERE r.deleted = false
                AND (
                    LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(r.displayName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            """)
    Page<Roles> search(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // =========================================================
    // USER ROLE QUERIES
    // =========================================================

    @Query("""
                SELECT DISTINCT r
                FROM Roles r
                JOIN r.users u
                WHERE u.id = :userId
                AND r.deleted = false
            """)
    Set<Roles> findRolesByUserId(@Param("userId") UUID userId);

    // =========================================================
    // PERMISSION QUERIES
    // =========================================================

    @Query("""
                SELECT DISTINCT r
                FROM Roles r
                JOIN r.permissions p
                WHERE p.name = :permissionName
                AND r.deleted = false
            """)
    List<Roles> findRolesByPermission(
            @Param("permissionName") String permissionName
    );

    // =========================================================
    // COUNTS
    // =========================================================

    @Query("""
                SELECT r.name, COUNT(DISTINCT u.id)
                FROM Roles r
                LEFT JOIN r.users u
                WHERE r.deleted = false
                GROUP BY r.name
            """)
    List<Object[]> roleUserCounts();

    // =========================================================
    // SOFT DELETE
    // =========================================================

    @Modifying
    @Query("""
                UPDATE Roles r
                SET r.deleted = true,
                    r.enabled = false
                WHERE r.id = :id
            """)
    void softDelete(@Param("id") UUID id);

    // =========================================================
    // STATUS
    // =========================================================

    @Modifying
    @Query("""
                UPDATE Roles r
                SET r.enabled = :status
                WHERE r.id = :id
            """)
    void updateStatus(
            @Param("id") UUID id,
            @Param("status") Boolean status
    );
}