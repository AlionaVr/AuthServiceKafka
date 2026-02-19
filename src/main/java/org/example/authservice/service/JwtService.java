package org.example.authservice.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
@Slf4j
public class JwtService {
    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.issuer}")
    private String issuer;

    @Value("${security.jwt.access-ttl-minutes}")
    private long ttlMinutes;

    private SecretKey cachedKey;

    private SecretKey key() {
        if (cachedKey == null) {
            try {
                byte[] bytes = Base64.getDecoder().decode(secret);
                cachedKey = Keys.hmacShaKeyFor(bytes);
                log.debug("JWT secret key successfully decoded.");
            } catch (IllegalArgumentException e) {
                log.error("Invalid JWT secret key format. Must be base64 encoded.", e);
                throw new IllegalStateException("Invalid JWT secret key format. Must be base64 encoded.", e);
            }
        }
        return cachedKey;
    }

    public String generateToken(String email) {
        log.debug("Generating JWT token for user {}", email);
        return Jwts.builder()
                .setSubject(email)
                .setIssuer(issuer)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttlMinutes * 60_000L))
                .signWith(key())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}