package com.bst.server.modules.authentication.exceptions.sub;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException() {
        super("You are not authorized to perform this action");
    }

    public UnauthorizedAccessException(String action) {
        super("You are not authorized to " + action);
    }
}