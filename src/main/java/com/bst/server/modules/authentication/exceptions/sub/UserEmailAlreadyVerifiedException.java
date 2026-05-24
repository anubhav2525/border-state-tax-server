package com.bst.server.modules.authentication.exceptions.sub;

public class UserEmailAlreadyVerifiedException extends RuntimeException {
    public UserEmailAlreadyVerifiedException(String email) {
        super("Email '" + email + "' is already verified");
    }
}
