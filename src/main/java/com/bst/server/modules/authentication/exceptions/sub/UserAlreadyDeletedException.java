package com.bst.server.modules.authentication.exceptions.sub;

public class UserAlreadyDeletedException extends RuntimeException {
    public UserAlreadyDeletedException(String email) {
        super("User '" + email + "' is already deleted");
    }
}
