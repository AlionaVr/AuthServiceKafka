package org.example.authservice.exception;

public class TooManyAttemptsException extends AuthException {
    public TooManyAttemptsException() {
        super("Too many verification attempts");
    }
}