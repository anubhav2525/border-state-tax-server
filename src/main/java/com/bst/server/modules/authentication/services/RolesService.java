package com.bst.server.modules.authentication.services;

import com.bst.server.common.utils.CustomResponse;
import com.bst.server.common.utils.PagedResponse;
import com.bst.server.modules.authentication.data.dtos.RolesRequest;
import com.bst.server.modules.authentication.data.dtos.RolesResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.util.UUID;

public interface RolesService {
    ResponseEntity<CustomResponse<RolesResponse.Detail>> create(RolesRequest.Create request, WebRequest webRequest);

    ResponseEntity<CustomResponse<RolesResponse.Detail>> getById(UUID id, WebRequest webRequest);

    ResponseEntity<CustomResponse<RolesResponse.Detail>> update(UUID id, RolesRequest.Update request, WebRequest webRequest);

    ResponseEntity<CustomResponse<PagedResponse<RolesResponse.Summary>>> search(RolesRequest.Search request, WebRequest webRequest);

    ResponseEntity<CustomResponse<RolesResponse.Detail>> enable(UUID id, WebRequest webRequest);

    ResponseEntity<CustomResponse<RolesResponse.Detail>> disable(UUID id, WebRequest webRequest);

    ResponseEntity<CustomResponse<Void>> softDelete(UUID id, WebRequest webRequest);
}
