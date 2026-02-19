package org.example.authservice.exception;

public class VerificationCodeExpiredException  extends AuthException{
    public VerificationCodeExpiredException() {
        super("Verification code expired");
    }
}
