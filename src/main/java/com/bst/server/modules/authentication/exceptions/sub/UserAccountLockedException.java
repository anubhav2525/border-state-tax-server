package com.bst.server.modules.authentication.exceptions.sub;

public class UserAccountLockedException extends RuntimeException {
    public UserAccountLockedException(String email) {
        super("User account '" + email + "' is locked. Please contact support or reset your password");
    }
}
