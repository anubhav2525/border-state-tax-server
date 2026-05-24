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
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permissions {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * e.g. "APPLICATION:CREATE"
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * e.g. "APPLICATION"
     */
    @Column(nullable = false, length = 50)
    private String resource;

    /**
     * e.g. "CREATE"
     */
    @Column(nullable = false, length = 50)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "deleted",nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @ManyToMany(mappedBy = "permissions")
    private Set<Roles> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}