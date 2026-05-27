package com.bst.server.modules.authentication.data.dtos;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.stereotype.Component;

@Component
public class PermissionsRequest {
   
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        private String name;
        private String resource;
        private String action;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update  {
        private String name;
        private String resource;
        private String action;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Search {
        private String name;

        private Boolean enabled;

        private Boolean deleted;

        @Min(value = 0, message = "Page index must be 0 or greater")
        @Builder.Default
        private Integer page = 0;

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size must not exceed 100")
        @Builder.Default
        private Integer size = 20;

        @Builder.Default
        private String sortBy = "createdAt";

        @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be 'asc' or 'desc'")
        @Builder.Default
        private String sortDir = "desc";
    }
}
