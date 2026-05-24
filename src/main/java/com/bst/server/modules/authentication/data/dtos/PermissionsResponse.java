package com.bst.server.modules.authentication.data.dtos;

import lombok.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
public class PermissionsResponse {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private String name;
        private String resource;
        private String action;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private String name;
        private String resource;
        private String action;
        private String description;
        private byte assignedRoleCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
