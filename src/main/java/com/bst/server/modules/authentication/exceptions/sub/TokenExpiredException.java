package com.bst.server.modules.authentication.exceptions.sub;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() {
        super("Token has expired. Please request a new one");
    }

    public TokenExpiredException(String tokenType) {
        super(tokenType + " token has expired. Please request a new one");
    }
}