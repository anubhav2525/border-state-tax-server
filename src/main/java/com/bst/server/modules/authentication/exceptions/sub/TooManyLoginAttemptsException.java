package com.bst.server.modules.authentication.exceptions.sub;

public class TooManyLoginAttemptsException extends RuntimeException {
    public TooManyLoginAttemptsException(int maxAttempts) {
        super("Account locked after " + maxAttempts + " failed login attempts. Please reset your password or contact support");
    }
}
