package com.bst.server.modules.authentication.exceptions.sub;

public class UserAccountAlreadyUnlockedException extends RuntimeException {
    public UserAccountAlreadyUnlockedException(String email) {
        super("User account '" + email + "' is already unlocked");
    }
}