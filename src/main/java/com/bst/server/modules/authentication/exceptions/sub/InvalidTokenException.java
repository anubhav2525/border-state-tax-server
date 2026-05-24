package com.bst.server.modules.authentication.exceptions.sub;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Token is invalid or malformed");
    }
    public InvalidTokenException(String message) {
        super(message);
    }
}
