package com.bst.server.modules.authentication.exceptions.sub;

public class UserAlreadyEnabledException extends RuntimeException {
    public UserAlreadyEnabledException(String email) {
        super("User '" + email + "' is already enabled");
    }
}
