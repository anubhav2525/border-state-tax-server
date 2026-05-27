package com.bst.server.modules.authentication.controllers;

import com.bst.server.common.constants.ApiVersion;
import com.bst.server.common.utils.CustomResponse;
import com.bst.server.common.utils.PagedResponse;
import com.bst.server.modules.authentication.data.dtos.UsersRequest;
import com.bst.server.modules.authentication.data.dtos.UsersResponse;
import com.bst.server.modules.authentication.services.UsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UsersController {
    private final UsersService usersService;

    @PostMapping(ApiVersion.V1 + "/auth/login")
    public ResponseEntity<CustomResponse<UsersResponse.Auth>> login(
            @Valid @RequestBody UsersRequest.Login request,
            WebRequest webRequest
    ) {
        return usersService.login(request, webRequest);
    }

    @PostMapping(ApiVersion.V1 + "/auth/refresh")
    public ResponseEntity<CustomResponse<UsersResponse.Auth>> refresh(
            @Valid @RequestBody UsersRequest.RefreshToken request,
            WebRequest webRequest
    ) {
        return usersService.refresh(request, webRequest);
    }

    @PostMapping(ApiVersion.V1 + "/users")
    public ResponseEntity<CustomResponse<UsersResponse.Detail>> create(
            @Valid @RequestBody UsersRequest.Create request,
            WebRequest webRequest
    ) {
        return usersService.create(request, webRequest);
    }

    @GetMapping(ApiVersion.V1 + "/users/{id}")
    public ResponseEntity<CustomResponse<UsersResponse.Detail>> getById(
            @PathVariable UUID id,
            WebRequest webRequest
    ) {
        return usersService.getById(id, webRequest);
    }

    @PatchMapping(ApiVersion.V1 + "/users/{id}")
    public ResponseEntity<CustomResponse<UsersResponse.Detail>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UsersRequest.Update request,
            WebRequest webRequest
    ) {
        return usersService.update(id, request, webRequest);
    }

    @GetMapping(ApiVersion.V1 + "/users/search")
    public ResponseEntity<CustomResponse<PagedResponse<UsersResponse.Summary>>> search(
            @Valid @ModelAttribute UsersRequest.Search request,
            WebRequest webRequest
    ) {
        return usersService.search(request, webRequest);
    }

    @PatchMapping(ApiVersion.V1 + "/users/{id}/enable")
    public ResponseEntity<CustomResponse<UsersResponse.Detail>> enable(@PathVariable UUID id, WebRequest webRequest) {
        return usersService.enable(id, webRequest);
    }

    @PatchMapping(ApiVersion.V1 + "/users/{id}/disable")
    public ResponseEntity<CustomResponse<UsersResponse.Detail>> disable(@PathVariable UUID id, WebRequest webRequest) {
        return usersService.disable(id, webRequest);
    }

    @DeleteMapping(ApiVersion.V1 + "/users/{id}")
    public ResponseEntity<CustomResponse<Void>> softDelete(@PathVariable UUID id, WebRequest webRequest) {
        return usersService.softDelete(id, webRequest);
    }
}
