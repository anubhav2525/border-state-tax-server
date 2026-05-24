package com.bst.server.modules.authentication.data.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Roles are dynamic — admins can create new roles at runtime
 * and assign any subset of the fixed {@link Permissions}s to them.
 */
@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Roles {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Unique technical name, e.g. "SUPER_ADMIN", "CUSTOMS_OFFICER"
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /**
     * Human-readable label, e.g. "Super Administrator"
     */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Permissions assigned to this role ──────────
    // Simple @JoinTable — no audit columns on this join.
    // Permission set is managed here (Role is the owning side).
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "permission_id", nullable = false)
    )
    @Builder.Default
    private Set<Permissions> permissions = new HashSet<>();

    // ── Users who have this role ───────────────────
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Users> users = new HashSet<>();

    // ── Convenience helpers ────────────────────────

    public void addPermission(Permissions permission) {
        this.permissions.add(permission);
    }

    public void removePermission(Permissions permission) {
        this.permissions.remove(permission);
    }
}
