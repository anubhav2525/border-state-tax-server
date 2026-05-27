package com.bst.server.modules.authentication.data.dtos;

import com.bst.server.modules.authentication.data.entities.Permissions;
import com.bst.server.modules.authentication.data.entities.Roles;
import com.bst.server.modules.authentication.data.entities.Users;
import lombok.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class UsersResponse {
    private final RolesResponse rolesResponse;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private UUID id;
        private String fullName;
        private String phone;
        private String email;
        private Boolean enabled;
        private Set<String> roles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private UUID id;
        private String fullName;
        private String phone;
        private String email;
        private Boolean enabled;
        private Boolean emailVerified;
        private Set<RolesResponse.Summary> roles;
        private Set<String> permissions;
        private LocalDateTime lastLoginAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Auth {
        private String accessToken;
        private String refreshToken;
        @Builder.Default
        private String tokenType = "Bearer";
        private Detail user;
    }

    public UsersResponse.Detail toDetail(Users user) {
        Set<RolesResponse.Summary> roles = user.getRoles().stream()
                .map(rolesResponse::toSummary)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> permissions = user.getRoles().stream()
                .filter(role -> Boolean.TRUE.equals(role.getEnabled()))
                .flatMap(role -> role.getPermissions().stream())
                .filter(permission -> Boolean.TRUE.equals(permission.getEnabled()))
                .map(Permissions::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return UsersResponse.Detail.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .enabled(user.getEnabled())
                .emailVerified(user.getIsEmailVerified())
                .roles(roles)
                .permissions(permissions)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public UsersResponse.Summary toSummary(Users user) {
        Set<String> roles = user
                .getRoles()
                .stream()
                .map(Roles::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return UsersResponse.Summary.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .enabled(user.getEnabled())
                .roles(roles)
                .build();
    }

}
