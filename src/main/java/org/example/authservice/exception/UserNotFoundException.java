package org.example.authservice.exception;

public class UserNotFoundException extends AuthException {
    public UserNotFoundException() {
        super("User not found");
    }
}