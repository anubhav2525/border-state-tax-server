package com.bst.server.modules.authentication.services;

import com.bst.server.common.utils.CustomResponse;
import com.bst.server.common.utils.PagedResponse;
import com.bst.server.modules.authentication.data.dtos.UsersRequest;
import com.bst.server.modules.authentication.data.dtos.UsersResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.util.UUID;

public interface UsersService {
    ResponseEntity<CustomResponse<UsersResponse.Auth>> login(UsersRequest.Login request, WebRequest webRequest);

    ResponseEntity<CustomResponse<UsersResponse.Auth>> refresh(UsersRequest.RefreshToken request, WebRequest webRequest);

    ResponseEntity<CustomResponse<UsersResponse.Detail>> create(UsersRequest.Create request, WebRequest webRequest);

    ResponseEntity<CustomResponse<UsersResponse.Detail>> getById(UUID id, WebRequest webRequest);

    ResponseEntity<CustomResponse<UsersResponse.Detail>> update(UUID id, UsersRequest.Update request, WebRequest webRequest);

    ResponseEntity<CustomResponse<PagedResponse<UsersResponse.Summary>>> search(UsersRequest.Search request, WebRequest webRequest);

    ResponseEntity<CustomResponse<UsersResponse.Detail>> enable(UUID id, WebRequest webRequest);

    ResponseEntity<CustomResponse<UsersResponse.Detail>> disable(UUID id, WebRequest webRequest);

    ResponseEntity<CustomResponse<Void>> softDelete(UUID id, WebRequest webRequest);
}
