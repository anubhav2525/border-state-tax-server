package com.bst.server.modules.authentication.data.dtos;

import com.bst.server.modules.authentication.data.entities.Permissions;
import lombok.*;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class PermissionsResponse {
    private final ModelMapper modelMapper;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private UUID id;
        private String name;
        private String resource;
        private String action;
        private Boolean enabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private UUID id;
        private String name;
        private String resource;
        private String action;
        private String description;
        private int assignedRoleCount;
        private Boolean enabled;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    public PermissionsResponse.Summary toSummary(Permissions permissions) {
        return modelMapper.map(permissions, PermissionsResponse.Summary.class);
    }

    public PermissionsResponse.Detail toDetail(Permissions permissions) {
        PermissionsResponse.Detail detail = modelMapper.map(permissions, PermissionsResponse.Detail.class);
        detail.setAssignedRoleCount(permissions.getRoles() == null ? 0 : permissions.getRoles().size());
        return detail;
    }
}
