package com.bst.server.modules.authentication.services;

import com.bst.server.common.utils.CustomResponse;
import com.bst.server.common.utils.PagedResponse;
import com.bst.server.modules.authentication.data.dtos.PermissionsRequest;
import com.bst.server.modules.authentication.data.dtos.PermissionsResponse;
import com.bst.server.modules.authentication.data.entities.Permissions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;
import java.util.UUID;

public interface PermissionsService {
    /**
     * Create a new permission.
     * Throws: ResourceAlreadyExistsException (name conflict)
     */
    ResponseEntity<CustomResponse<PermissionsResponse.Detail>> create(
            PermissionsRequest.Create request, WebRequest webRequest);

    /**
     * Get permission by ID — only non-deleted.
     * Throws: ResourceNotFoundException
     */
    ResponseEntity<CustomResponse<PermissionsResponse.Detail>> getById(UUID id, WebRequest webRequest);

    /**
     * Update name / description / enabled flag.
     * Throws: ResourceNotFoundException
     * ResourceAlreadyExistsException (name conflict with another record)
     */
    ResponseEntity<CustomResponse<PermissionsResponse.Detail>> update(
            UUID id, PermissionsRequest.Update request, WebRequest webRequest);

    /**
     * Throws: ResourceNotFoundException
     */
    ResponseEntity<CustomResponse<PagedResponse<PermissionsResponse.Summary>>> search(
            PermissionsRequest.Search request, WebRequest webRequest);

    /**
     * Throws: ResourceNotFoundException
     * PermissionAlreadyEnabledException
     */
    ResponseEntity<CustomResponse<PermissionsResponse.Detail>> enable(UUID id, WebRequest webRequest);

    /**
     * Throws: ResourceNotFoundException
     * PermissionAlreadyDisabledException
     */
    ResponseEntity<CustomResponse<PermissionsResponse.Detail>> disable(UUID id, WebRequest webRequest);

    ResponseEntity<CustomResponse<Void>> softDelete(UUID id, WebRequest webRequest);

    ResponseEntity<CustomResponse<List<PermissionsResponse.Summary>>> getAllActivePermissions(WebRequest webRequest);

}
