package com.bst.server.common.exceptions.sub;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.ALREADY_REPORTED)
public class ResourceAlreadyEnabledException extends RuntimeException {
    public ResourceAlreadyEnabledException(String message) {
        super(message);
    }
}
