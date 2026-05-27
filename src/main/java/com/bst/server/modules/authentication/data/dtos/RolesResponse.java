package com.bst.server.modules.authentication.data.dtos;

import com.bst.server.modules.authentication.data.entities.Roles;
import lombok.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RolesResponse {
    private final PermissionsResponse permissionsResponse;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private UUID id;
        private String name;
        private String displayName;
        private Boolean enabled;
        private int permissionCount;
        private int userCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private UUID id;
        private String name;
        private String displayName;
        private String description;
        private Boolean enabled;
        private Set<PermissionsResponse.Summary> permissions;
        private int userCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }


    public RolesResponse.Detail toDetail(Roles role) {
        Set<PermissionsResponse.Summary> permissions = role
                .getPermissions()
                .stream()
                .map(permissionsResponse::toSummary)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return RolesResponse.Detail.builder()
                .id(role.getId())
                .name(role.getName())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .enabled(role.getEnabled())
                .permissions(permissions)
                .userCount(role.getUsers() == null ? 0 : role.getUsers().size())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    public RolesResponse.Summary toSummary(Roles role) {
        int permissionCount = role.getPermissions() == null ? 0 : role.getPermissions().size();
        int userCount = role.getUsers() == null ? 0 : role.getUsers().size();
        return RolesResponse.Summary.builder()
                .id(role.getId())
                .name(role.getName())
                .displayName(role.getDisplayName())
                .enabled(role.getEnabled())
                .permissionCount(permissionCount)
                .userCount(userCount)
                .build();
    }
}
