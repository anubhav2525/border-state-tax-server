package com.bst.server.modules.authentication.data.mappers;

import com.bst.server.modules.authentication.data.dtos.PermissionsRequest;
import com.bst.server.modules.authentication.data.dtos.PermissionsResponse;
import com.bst.server.modules.authentication.data.entities.Permissions;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
public class PermissionMapper {
    private final ModelMapper modelMapper;

    public Permissions toPermissions(PermissionsRequest.Create permission) {
        Permissions permissions = modelMapper.map(permission, Permissions.class);
        permissions.setCreatedAt(LocalDateTime.now());
        permissions.setUpdatedAt(LocalDateTime.now());
        return permissions;
    }

    public Permissions toPermissions(PermissionsRequest.Update permission) {
        Permissions permissions = modelMapper.map(permission, Permissions.class);
        permissions.setCreatedAt(LocalDateTime.now());
        permissions.setUpdatedAt(LocalDateTime.now());
        return permissions;
    }

    public PermissionsResponse.Summary toSummary(Permissions permissions) {
        return modelMapper.map(permissions, PermissionsResponse.Summary.class);
    }

    public PermissionsResponse.Detail toDetail(Permissions permissions, byte assignedRoleCount) {
        PermissionsResponse.Detail detail = modelMapper.map(permissions, PermissionsResponse.Detail.class);
        detail.setAssignedRoleCount(assignedRoleCount);
        return detail;
    }
}
