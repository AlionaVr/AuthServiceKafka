package org.example.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.authservice.dto.RegistrationRequest;
import org.example.authservice.dto.VerifyRequest;
import org.example.authservice.entity.Status;
import org.example.authservice.entity.User;
import org.example.authservice.exception.InvalidVerificationCodeException;
import org.example.authservice.exception.TooManyAttemptsException;
import org.example.authservice.exception.VerificationCodeExpiredException;
import org.example.authservice.exception.VerificationCodeNotFoundException;
import org.example.authservice.service.AuthService;
import org.example.authservice.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthService authService;
    @MockitoBean
    JwtService jwtService;

    @Test
    @WithMockUser
    void register_validEmail_returns200() throws Exception {
        RegistrationRequest req = new RegistrationRequest();
        req.setEmail("user@example.com");

        doNothing().when(authService).register("user@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("user@example.com")));
    }

    @Test
    @WithMockUser
    void register_invalidEmail_returns400() throws Exception {
        RegistrationRequest req = new RegistrationRequest();
        req.setEmail("not-an-email");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @WithMockUser
    void register_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void verify_validCode_returnsToken() throws Exception {
        VerifyRequest req = new VerifyRequest();
        req.setEmail("user@example.com");
        req.setCode("123456");

        when(authService.verify("user@example.com", "123456")).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authToken").value("jwt-token"));
    }

    @Test
    @WithMockUser
    void verify_invalidCodeFormat_returns400() throws Exception {
        VerifyRequest req = new VerifyRequest();
        req.setEmail("user@example.com");
        req.setCode("12345");

        mockMvc.perform(post("/api/auth/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @WithMockUser
    void verify_alphaCode_returns400() throws Exception {
        VerifyRequest req = new VerifyRequest();
        req.setEmail("user@example.com");
        req.setCode("12345a");

        mockMvc.perform(post("/api/auth/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void verify_codeNotFound_returns404() throws Exception {
        VerifyRequest req = new VerifyRequest();
        req.setEmail("user@example.com");
        req.setCode("000000");

        when(authService.verify(any(), any())).thenThrow(new VerificationCodeNotFoundException());

        mockMvc.perform(post("/api/auth/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void verify_expiredCode_returns410() throws Exception {
        VerifyRequest req = new VerifyRequest();
        req.setEmail("user@example.com");
        req.setCode("000000");

        when(authService.verify(any(), any())).thenThrow(new VerificationCodeExpiredException());

        mockMvc.perform(post("/api/auth/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isGone());
    }

    @Test
    @WithMockUser
    void verify_wrongCode_returns401() throws Exception {
        VerifyRequest req = new VerifyRequest();
        req.setEmail("user@example.com");
        req.setCode("000000");

        when(authService.verify(any(), any())).thenThrow(new InvalidVerificationCodeException());

        mockMvc.perform(post("/api/auth/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void verify_tooManyAttempts_returns429() throws Exception {
        VerifyRequest req = new VerifyRequest();
        req.setEmail("user@example.com");
        req.setCode("000000");

        when(authService.verify(any(), any())).thenThrow(new TooManyAttemptsException());

        mockMvc.perform(post("/api/auth/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests());
    }


    @Test
    @WithMockUser
    void getUsers_authenticated_returns200() throws Exception {
        User user = User.builder()
                .id(1L)
                .email("a@b.com")
                .status(Status.ACTIVE)
                .build();
        when(authService.getUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("a@b.com"));
    }

    @Test
    void getUsers_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().is(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is(401),
                        org.hamcrest.Matchers.is(403)
                )));
    }
}