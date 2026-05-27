package com.bst.server.modules.authentication.controllers;

import com.bst.server.common.constants.ApiVersion;
import com.bst.server.common.utils.CustomResponse;
import com.bst.server.common.utils.PagedResponse;
import com.bst.server.modules.authentication.data.dtos.RolesRequest;
import com.bst.server.modules.authentication.data.dtos.RolesResponse;
import com.bst.server.modules.authentication.services.RolesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.UUID;

@RestController
@RequestMapping(ApiVersion.V1 + "/roles")
@RequiredArgsConstructor
public class RolesController {
    private final RolesService rolesService;

    @PostMapping
    public ResponseEntity<CustomResponse<RolesResponse.Detail>> create(
            @Valid @RequestBody RolesRequest.Create request,
            WebRequest webRequest
    ) {
        return rolesService.create(request, webRequest);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<RolesResponse.Detail>> getById(@PathVariable UUID id, WebRequest webRequest) {
        return rolesService.getById(id, webRequest);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CustomResponse<RolesResponse.Detail>> update(
            @PathVariable UUID id,
            @Valid @RequestBody RolesRequest.Update request,
            WebRequest webRequest
    ) {
        return rolesService.update(id, request, webRequest);
    }

    @GetMapping("/search")
    public ResponseEntity<CustomResponse<PagedResponse<RolesResponse.Summary>>> search(
            @Valid @ModelAttribute RolesRequest.Search request,
            WebRequest webRequest
    ) {
        return rolesService.search(request, webRequest);
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<CustomResponse<RolesResponse.Detail>> enable(@PathVariable UUID id, WebRequest webRequest) {
        return rolesService.enable(id, webRequest);
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<CustomResponse<RolesResponse.Detail>> disable(@PathVariable UUID id, WebRequest webRequest) {
        return rolesService.disable(id, webRequest);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CustomResponse<Void>> softDelete(@PathVariable UUID id, WebRequest webRequest) {
        return rolesService.softDelete(id, webRequest);
    }
}
