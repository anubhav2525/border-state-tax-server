package com.bst.server.modules.authentication.exceptions.sub;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException() {
        super("Current password is incorrect");
    }
}
