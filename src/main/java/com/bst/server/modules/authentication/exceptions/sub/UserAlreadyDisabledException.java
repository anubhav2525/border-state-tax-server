package com.bst.server.modules.authentication.exceptions.sub;

public class UserAlreadyDisabledException extends RuntimeException {
    public UserAlreadyDisabledException(String email) {
        super("User '" + email + "' is already disabled");
    }
}
