package com.bst.server.common.exceptions.sub;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
public class ResourceOperationNotAllowed extends RuntimeException {
    public ResourceOperationNotAllowed(String message) {
        super(message);
    }
}
