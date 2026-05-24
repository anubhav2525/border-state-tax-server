package com.bst.server.common.exceptions.sub;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ResourceNotDeletedException extends RuntimeException {
    public ResourceNotDeletedException(String message) {
        super(message);
    }
}
