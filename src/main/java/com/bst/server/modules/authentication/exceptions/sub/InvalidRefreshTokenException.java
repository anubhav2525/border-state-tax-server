package com.bst.server.modules.authentication.exceptions.sub;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Refresh token is invalid or has already been used");
    }
}