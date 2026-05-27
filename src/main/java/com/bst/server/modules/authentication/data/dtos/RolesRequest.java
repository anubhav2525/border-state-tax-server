package com.bst.server.modules.authentication.data.dtos;

import lombok.*;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RolesRequest {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        @NotBlank
        private String name;
        @NotBlank
        private String displayName;
        private String description;
        private Set<UUID> permissionIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        private String name;
        private String displayName;
        private String description;
        private Set<UUID> permissionIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Search {
        private String keyword;
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
}
