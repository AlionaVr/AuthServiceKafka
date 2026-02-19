package org.example.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.entity.Status;
import org.example.authservice.entity.User;
import org.example.authservice.entity.VerificationCode;
import org.example.authservice.exception.*;
import org.example.authservice.repository.UserRepository;
import org.example.authservice.repository.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final JwtService jwt;

    @Value("${app.verification.code-expiration-minutes}")
    private int codeExpirationMinutes;

    @Value("${app.code-secret}")
    private String codeSecret;

    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom random = new SecureRandom();


    @Transactional
    public void register(String email) {
        if (!userRepository.existsByEmail(email)) {
            User user = User.builder()
                    .email(email)
                    .status(Status.PENDING)
                    .build();
            userRepository.save(user);
            log.info("User is created: {}", email);
        }

        String code = generateCode();

        VerificationCode verificationCode = VerificationCode.builder()
                .email(email)
                .codeHash(code)
                .expiresAt(LocalDateTime.now().plusMinutes(codeExpirationMinutes))
                .build();
        verificationCodeRepository.save(verificationCode);
        // sending code to client

    }


    @Transactional
    public String verify(String email, String code) {
        VerificationCode verificationCode = verificationCodeRepository
                .findTopByEmailAndUsedFalseOrderByIdDesc(email)
                .orElseThrow(VerificationCodeNotFoundException::new);

        if (verificationCode.isExpired()) {
            throw new VerificationCodeExpiredException();
        }

        verificationCode.setAttempts(verificationCode.getAttempts() + 1);
        if (verificationCode.getAttempts() > MAX_ATTEMPTS) {
            throw new TooManyAttemptsException();
        }

        if (!matches(code, verificationCode.getCodeHash())) {
            throw new InvalidVerificationCodeException();
        }

        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
        log.info("User {} is verified", email);
        return jwt.generateToken(email);
    }

    @Transactional (readOnly = true)
    public List<User> getUsers() {
       return userRepository.findAll();
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String hashCode(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((code + codeSecret).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash verification code", e);
        }
    }

    private boolean matches(String rawCode, String hash) {
        return hashCode(rawCode).equals(hash);
    }
}