package org.example.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "ZmFrZS1iYXNlNjQtc2VjcmV0LXN0cmluZy1mb3ItZGVtbw==";
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "issuer", "auth-service");
        ReflectionTestUtils.setField(jwtService, "ttlMinutes", 15L);
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        String token = jwtService.generateToken("user@example.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void getEmailFromToken_returnsCorrectEmail() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email);

        assertThat(jwtService.getEmailFromToken(token)).isEqualTo(email);
    }

    @Test
    void generateToken_differentEmails_differentTokens() {
        String token1 = jwtService.generateToken("a@example.com");
        String token2 = jwtService.generateToken("b@example.com");
        assertThat(token1).isNotEqualTo(token2);
    }

}