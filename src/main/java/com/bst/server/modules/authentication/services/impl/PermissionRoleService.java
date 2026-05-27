package com.bst.server.modules.authentication.services.impl;

import com.bst.server.common.exceptions.sub.ResourceNotExistsException;
import com.bst.server.modules.authentication.data.entities.Permissions;
import com.bst.server.modules.authentication.repositories.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class PermissionRoleService {
    private final PermissionRepository permissionRepository;

    public Set<Permissions> resolvePermissions(Set<UUID> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty())
            return new HashSet<>();

        Set<Permissions> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));
        Set<UUID> foundIds = permissions.stream().map(Permissions::getId).collect(Collectors.toSet());
        List<UUID> missing = permissionIds.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missing.isEmpty()) throw new ResourceNotExistsException("Permissions not found: " + missing);
        return permissions;
    }

}
