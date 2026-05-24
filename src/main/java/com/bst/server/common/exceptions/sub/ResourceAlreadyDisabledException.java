package com.bst.server.common.exceptions.sub;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceAlreadyDisabledException extends RuntimeException {
    public ResourceAlreadyDisabledException(String message) {
        super(message);
    }
}
