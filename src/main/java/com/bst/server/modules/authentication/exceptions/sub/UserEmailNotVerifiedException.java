package com.bst.server.modules.authentication.exceptions.sub;

public class UserEmailNotVerifiedException extends RuntimeException {
    public UserEmailNotVerifiedException(String email) {
        super("Email '" + email + "' is not verified. Please verify your email to continue");
    }
}