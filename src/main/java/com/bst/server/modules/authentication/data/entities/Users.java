package com.bst.server.modules.authentication.data.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 15)
    @ToString.Include
    private String phone;

    @Column(unique = true, length = 40)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // ── Account state ──────────────────────────────
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    // ── Security tracking ──────────────────────────
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    /**
     * NULL means account is not locked
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at", nullable = false)
    @Builder.Default
    private LocalDateTime passwordChangedAt = LocalDateTime.now();

    // ── Refresh token (JWT rotation) ───────────────
    /**
     * Stored hashed — never plain text
     */
    @Column(name = "refresh_token_hash")
    private String refreshTokenHash;

    @Column(name = "refresh_token_expires_at")
    private LocalDateTime refreshTokenExpiresAt;

    // ── Audit ──────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Relationships ──────────────────────────────

    /**
     * Roles assigned to this user.
     * Simple @JoinTable — no audit columns on this join.
     * User is the owning side.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "role_id", nullable = false)
    )
    @Builder.Default
    private Set<Roles> roles = new HashSet<>();

    // ── Convenience helpers ────────────────────────

    public void assignRole(Roles role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }

    public void revokeRole(Roles role) {
        this.roles.remove(role);
        role.getUsers().remove(this);
    }

    /**
     * Flat permission check across all assigned roles
     */
    public boolean hasPermission(String permissionName) {
        return this.roles.stream()
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                .flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> p.getName().equals(permissionName));
    }
}