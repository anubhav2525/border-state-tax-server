package com.bst.server.modules.authentication.controllers;

import com.bst.server.common.constants.ApiVersion;
import com.bst.server.common.utils.CustomResponse;
import com.bst.server.common.utils.PagedResponse;
import com.bst.server.modules.authentication.data.dtos.PermissionsRequest;
import com.bst.server.modules.authentication.data.dtos.PermissionsResponse;
import com.bst.server.modules.authentication.services.PermissionsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiVersion.V1 + "/permissions")
@RequiredArgsConstructor
public class PermissionsController {
    private final PermissionsService permissionsService;

    @PostMapping
    public ResponseEntity<CustomResponse<PermissionsResponse.Detail>> create(
            @Valid @RequestBody PermissionsRequest.Create request,
            WebRequest webRequest
    ) {
        return permissionsService.create(request, webRequest);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<PermissionsResponse.Detail>> getById(@PathVariable UUID id, WebRequest webRequest) {
        return permissionsService.getById(id, webRequest);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CustomResponse<PermissionsResponse.Detail>> update(
            @PathVariable UUID id,
            @Valid @RequestBody PermissionsRequest.Update request,
            WebRequest webRequest
    ) {
        return permissionsService.update(id, request, webRequest);
    }

    @GetMapping("/search")
    public ResponseEntity<CustomResponse<PagedResponse<PermissionsResponse.Summary>>> search(
            @Valid @ModelAttribute PermissionsRequest.Search request,
            WebRequest webRequest
    ) {
        return permissionsService.search(request, webRequest);
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<CustomResponse<PermissionsResponse.Detail>> enable(@PathVariable UUID id, WebRequest webRequest) {
        return permissionsService.enable(id, webRequest);
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<CustomResponse<PermissionsResponse.Detail>> disable(@PathVariable UUID id, WebRequest webRequest) {
        return permissionsService.disable(id, webRequest);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CustomResponse<Void>> softDelete(@PathVariable UUID id, WebRequest webRequest) {
        return permissionsService.softDelete(id, webRequest);
    }

    @GetMapping("/active")
    public ResponseEntity<CustomResponse<List<PermissionsResponse.Summary>>> getAllActivePermission(WebRequest webRequest) {
        return permissionsService.getAllActivePermissions(webRequest);
    }
}
