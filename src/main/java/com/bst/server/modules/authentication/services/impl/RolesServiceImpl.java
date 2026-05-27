package com.bst.server.modules.authentication.services.impl;

import com.bst.server.common.exceptions.sub.*;
import com.bst.server.common.utils.*;
import com.bst.server.modules.authentication.data.dtos.*;
import com.bst.server.modules.authentication.data.entities.Roles;
import com.bst.server.modules.authentication.repositories.RoleRepository;
import com.bst.server.modules.authentication.services.RolesService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.WebRequest;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RolesServiceImpl implements RolesService {
    private static final String RESOURCE = "Role";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name", "displayName", "createdAt", "updatedAt", "enabled"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final RoleRepository roleRepository;
    private final PermissionRoleService permissionRoleService;
    private final CreateResponseEntity createResponseEntity;
    private final RolesResponse rolesResponse;
    private final BuildPageable buildPageable;
    private final StringOperation stringOperation;

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<RolesResponse.Detail>> create(
            RolesRequest.Create request,
            WebRequest webRequest) {
        String name = normalizeName(request.getName());

        if (roleRepository.existsByNameAndDeletedFalse(name)) {
            throw new ResourceAlreadyExistsException(
                    RESOURCE + " already exists with name: " + name);
        }

        Roles role = Roles.builder()
                .name(name)
                .displayName(requiredTrim(request.getDisplayName(), "displayName"))
                .description(stringOperation.trimOrNull(request.getDescription()))
                .enabled(true)
                .deleted(false)
                .permissions(permissionRoleService.resolvePermissions(request.getPermissionIds()))
                .build();

        roleRepository.save(role);
        return createResponseEntity.buildResponse(
                RESOURCE + " created successfully",
                rolesResponse.toDetail(role),
                HttpStatus.CREATED,
                webRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CustomResponse<RolesResponse.Detail>> getById(
            UUID id, WebRequest webRequest) {
        Roles roles = getRole(id);
        return createResponseEntity.buildResponse(
                RESOURCE + " fetched successfully",
                rolesResponse.toDetail(roles),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<RolesResponse.Detail>> update(
            UUID id,
            RolesRequest.Update request,
            WebRequest webRequest) {
        Roles role = getRole(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            String name = normalizeName(request.getName());
            roleRepository.findByNameAndDeletedFalse(name)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResourceAlreadyExistsException(RESOURCE + " already exists with name: " + name);
                    });
            role.setName(name);
        }
        if (request.getDisplayName() != null)
            role.setDisplayName(requiredTrim(request.getDisplayName(), "displayName"));
        if (request.getDescription() != null)
            role.setDescription(stringOperation.trimOrNull(request.getDescription()));
        if (request.getPermissionIds() != null)
            role.setPermissions(
                    permissionRoleService.resolvePermissions(request.getPermissionIds()));

        roleRepository.save(role);
        return createResponseEntity.buildResponse(
                RESOURCE + " updated successfully",
                rolesResponse.toDetail(role),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CustomResponse<PagedResponse<RolesResponse.Summary>>> search(
            RolesRequest.Search request, WebRequest webRequest) {

        Pageable pageable = buildPageable.build(
                request.getPage(), request.getSize(),
                request.getSortBy(), request.getSortDir(),
                ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD);

        Page<Roles> page = request.getKeyword() != null && !request.getKeyword().isBlank()
                ? roleRepository.search(request.getKeyword(), pageable)
                : roleRepository.findAllWithRelations(pageable);

        return createResponseEntity.buildResponse(
                RESOURCE + " list fetched successfully",
                PagedResponse.of(page.map(rolesResponse::toSummary)),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<RolesResponse.Detail>> enable(UUID id, WebRequest webRequest) {
        Roles role = getRole(id);

        if (Boolean.TRUE.equals(role.getEnabled()))
            throw new ResourceAlreadyEnabledException(RESOURCE + " is already enabled");

        role.setEnabled(true);
        roleRepository.save(role);
        return createResponseEntity.buildResponse(
                RESOURCE + " enabled successfully",
                rolesResponse.toDetail(role),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<RolesResponse.Detail>> disable(UUID id, WebRequest webRequest) {
        Roles role = getRole(id);

        if (Boolean.FALSE.equals(role.getEnabled()))
            throw new ResourceAlreadyDisabledException(RESOURCE + " is already disabled");

        role.setEnabled(false);
        roleRepository.save(role);
        return createResponseEntity.buildResponse(
                RESOURCE + " disabled successfully",
                rolesResponse.toDetail(role),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<Void>> softDelete(UUID id, WebRequest webRequest) {
        Roles role = getRole(id);

        if (role.getUsers() != null && !role.getUsers().isEmpty()) {
            throw new ResourceInUseException(RESOURCE + " is assigned to users and cannot be deleted");
        }

        role.setDeleted(true);
        role.setEnabled(false);
        roleRepository.save(role);

        return createResponseEntity.buildResponse(
                RESOURCE + " deleted successfully",

                HttpStatus.OK,
                webRequest);
    }


    // Private helper
    private Roles getRole(UUID id) {
        return roleRepository.findWithPermissions(id)
                .orElseThrow(() -> new ResourceNotExistsException(RESOURCE + " not found with id: " + id));
    }

    private String normalizeName(String value) {
        return requiredTrim(value, "name").toUpperCase().replaceAll("\\s+", "_");
    }

    private String requiredTrim(String value, String field) {
        String trimmed = stringOperation.trimOrNull(value);
        if (trimmed == null) throw new ResourceValidationException(field + " is required");
        return trimmed;
    }

}
