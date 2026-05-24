package com.bst.server.modules.authentication.exceptions.sub;

public class PasswordSameAsOldException extends RuntimeException {
    public PasswordSameAsOldException() {
        super("New password must be different from the current password");
    }
}
