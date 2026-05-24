package com.bst.server.modules.authentication.exceptions.sub;

public class PasswordMismatchException extends RuntimeException {
    public PasswordMismatchException() {
        super("New password and confirm password do not match");
    }
}