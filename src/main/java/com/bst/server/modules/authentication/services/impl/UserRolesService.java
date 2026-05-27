package com.bst.server.modules.authentication.services.impl;

import com.bst.server.common.exceptions.sub.ResourceNotExistsException;
import com.bst.server.modules.authentication.data.entities.Roles;
import com.bst.server.modules.authentication.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserRolesService {
    private final RoleRepository roleRepository;

    public Set<Roles> resolveRoles(Set<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty())
            return new HashSet<>();
        Set<Roles> roles = new HashSet<>(roleRepository.findAllById(roleIds));
        Set<UUID> foundIds = roles.stream().map(Roles::getId).collect(Collectors.toSet());
        List<UUID> missing = roleIds.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missing.isEmpty()) throw new ResourceNotExistsException("Roles not found: " + missing);
        return roles;
    }
}
