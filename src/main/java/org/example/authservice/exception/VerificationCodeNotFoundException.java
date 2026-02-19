package org.example.authservice.exception;

public class VerificationCodeNotFoundException extends AuthException {
    public VerificationCodeNotFoundException() {
        super("Verification code not found");
    }
}