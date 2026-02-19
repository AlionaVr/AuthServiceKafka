package org.example.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.authservice.dto.RegistrationRequest;
import org.example.authservice.dto.TokenResponse;
import org.example.authservice.dto.VerifyRequest;
import org.example.authservice.entity.User;
import org.example.authservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationRequest request) {
        authService.register(request.getEmail());
        return ResponseEntity.ok("Verification code is sent to  " + request.getEmail());
    }

    @PostMapping("/auth/verify")
    public ResponseEntity<TokenResponse> verify(@Valid @RequestBody VerifyRequest request) {
        String token = authService.verify(request.getEmail(), request.getCode());
        return ResponseEntity.ok(new TokenResponse(token));
    }

    @GetMapping("/protected")
    public ResponseEntity<List<User>> getUsers () {
        return ResponseEntity.ok(authService.getUsers());
    }
}