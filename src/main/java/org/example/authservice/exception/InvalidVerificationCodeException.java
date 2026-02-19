package org.example.authservice.exception;

public class InvalidVerificationCodeException extends AuthException {
    public InvalidVerificationCodeException() {
        super("Invalid verification code");
    }
}