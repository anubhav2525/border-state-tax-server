package com.bst.server.modules.authentication.exceptions.sub;

public class UserAccountAlreadyLockedException extends RuntimeException {
    public UserAccountAlreadyLockedException(String email) {
        super("User account '" + email + "' is already locked");
    }
}
