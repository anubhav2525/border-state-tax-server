package com.bst.server.modules.authentication.data.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

public class UsersRequest {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        @NotBlank
        private String fullName;
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{10,15}$")
        private String phone;
        @Email
        private String email;
        @NotBlank
        @Size(min = 8)
        private String password;
        private Set<UUID> roleIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        private String fullName;
        @Pattern(regexp = "^\\+?[0-9]{10,15}$")
        private String phone;
        @Email
        private String email;
        private Set<UUID> roleIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Search {
        private String keyword;
        private String roleName;
        private Boolean enabled;
        @Builder.Default
        private Integer page = 0;
        @Builder.Default
        private Integer size = 20;
        @Builder.Default
        private String sortBy = "createdAt";
        @Builder.Default
        private String sortDir = "desc";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Login {
        @NotBlank
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshToken {
        @NotBlank
        private String refreshToken;
    }
}
