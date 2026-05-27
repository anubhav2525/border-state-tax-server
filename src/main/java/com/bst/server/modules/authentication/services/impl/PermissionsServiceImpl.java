package com.bst.server.modules.authentication.services.impl;

import com.bst.server.common.exceptions.sub.*;
import com.bst.server.common.utils.*;
import com.bst.server.modules.authentication.data.dtos.PermissionsRequest;
import com.bst.server.modules.authentication.data.dtos.PermissionsResponse;
import com.bst.server.modules.authentication.data.entities.Permissions;
import com.bst.server.modules.authentication.repositories.PermissionRepository;
import com.bst.server.modules.authentication.services.PermissionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionsServiceImpl implements PermissionsService {
    private static final String RESOURCE = "Permission";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name", "resource", "action", "createdAt", "updatedAt", "enabled");
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final PermissionRepository permissionRepository;
    private final CreateResponseEntity createResponseEntity;
    private final BuildPageable buildPageable;
    private final StringOperation stringOperation;
    private final PermissionsResponse permissionsResponse;

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<PermissionsResponse.Detail>> create(
            PermissionsRequest.Create request, WebRequest webRequest) {
        String resource = normalize(request.getResource());
        String action = normalize(request.getAction());

        String name = request.getName() == null || request.getName().isBlank()
                ? resource + ":" + action
                : normalize(request.getName());

        if (permissionRepository.existsByNameAndDeletedFalse(name)
                || permissionRepository.existsByResourceAndActionAndDeletedFalse(resource, action)) {
            throw new ResourceAlreadyExistsException(RESOURCE + " already exists");
        }

        Permissions permission = Permissions.builder()
                .name(name)
                .resource(resource)
                .action(action)
                .description(stringOperation.trimOrNull(request.getDescription()))
                .enabled(true)
                .deleted(false)
                .build();

        permissionRepository.save(permission);
        return createResponseEntity.buildResponse(
                RESOURCE + " created successfully",
                permissionsResponse.toDetail(permission),
                HttpStatus.CREATED,
                webRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CustomResponse<PermissionsResponse.Detail>> getById(
            UUID id, WebRequest webRequest) {
        Permissions permissions = getPermission(id);
        return createResponseEntity.buildResponse(
                RESOURCE + " fetched successfully",
                permissionsResponse.toDetail(getPermission(id)),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<PermissionsResponse.Detail>> update(
            UUID id, PermissionsRequest.Update request, WebRequest webRequest) {
        Permissions permission = getPermission(id);
        String resource = request.getResource() == null ? permission.getResource() : normalize(request.getResource());
        String action = request.getAction() == null ? permission.getAction() : normalize(request.getAction());
        String name = request.getName() == null || request.getName().isBlank() ? resource + ":" + action : normalize(request.getName());

        permissionRepository.findByNameAndDeletedFalse(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException(RESOURCE + " already exists with name: " + name);
                });
        permissionRepository.findByResourceAndActionAndDeletedFalse(resource, action)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException(RESOURCE + " already exists for resource/action");
                });

        permission.setName(name);
        permission.setResource(resource);
        permission.setAction(action);
        permission.setDescription(stringOperation.trimOrNull(request.getDescription()));

        permissionRepository.save(permission);
        return createResponseEntity.buildResponse(
                RESOURCE + " updated successfully",
                permissionsResponse.toDetail(permission),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CustomResponse<PagedResponse<PermissionsResponse.Summary>>> search(
            PermissionsRequest.Search request, WebRequest webRequest) {
        Pageable pageable = buildPageable.build(
                request.getPage(), request.getSize(),
                request.getSortBy(), request.getSortDir(),
                ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD);

        Page<Permissions> page = request.getName() != null && !request.getName().isBlank()
                ? permissionRepository.search(request.getName(), pageable)
                : request.getEnabled() != null
                  ? permissionRepository.findAllByEnabledAndDeletedFalse(request.getEnabled(), pageable)
                  : permissionRepository.findAllByDeletedFalse(pageable);

        return createResponseEntity.buildResponse(
                RESOURCE + " list fetched successfully",
                PagedResponse.of(page.map(permissionsResponse::toSummary)),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<PermissionsResponse.Detail>> enable(UUID id, WebRequest webRequest) {
        Permissions permission = getPermission(id);
        if (Boolean.TRUE.equals(permission.getEnabled()))
            throw new ResourceAlreadyEnabledException(RESOURCE + " is already enabled");
        permission.setEnabled(true);
        permissionRepository.save(permission);

        return createResponseEntity.buildResponse(
                RESOURCE + " enabled successfully",
                permissionsResponse.toDetail(permission),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<PermissionsResponse.Detail>> disable(UUID id, WebRequest webRequest) {
        Permissions permission = getPermission(id);
        if (Boolean.FALSE.equals(permission.getEnabled()))
            throw new ResourceAlreadyDisabledException(RESOURCE + " is already disabled");

        permission.setEnabled(false);
        permissionRepository.save(permission);

        return createResponseEntity.buildResponse(
                RESOURCE + " disabled successfully",
                permissionsResponse.toDetail(permission),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<Void>> softDelete(UUID id, WebRequest webRequest) {
        Permissions permission = getPermission(id);

        permission.setDeleted(true);
        permission.setEnabled(false);
        permissionRepository.save(permission);

        return createResponseEntity.buildResponse(
                RESOURCE + " deleted successfully",
                HttpStatus.OK,
                webRequest);
    }

    @Override
    public ResponseEntity<CustomResponse<List<PermissionsResponse.Summary>>> getAllActivePermissions(WebRequest webRequest) {
        List<PermissionsResponse.Summary> summaries = permissionRepository
                .findAllDeletedFalse()
                .stream()
                .map(permissionsResponse::toSummary)
                .toList();

        if (summaries.isEmpty())
            throw new ResourceNotExistsException("No permissions found");

        return createResponseEntity
                .buildResponse(
                        "Permissions found",
                        summaries,
                        HttpStatus.OK,
                        webRequest
                );
    }

    // ===============================
    // Private Helper
    // ===============================

    private Permissions getPermission(UUID id) {
        return permissionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotExistsException(RESOURCE + " not found"));
    }

    private String normalize(String value) {
        String normalized = stringOperation.trimOrNull(value);
        if (normalized == null) throw new ResourceValidationException("Permission resource/action/name is required");
        return normalized.toUpperCase();
    }

}
