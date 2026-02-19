package org.example.authservice.service;

import org.example.authservice.entity.Status;
import org.example.authservice.entity.User;
import org.example.authservice.entity.VerificationCode;
import org.example.authservice.exception.*;
import org.example.authservice.repository.UserRepository;
import org.example.authservice.repository.VerificationCodeRepository;
import org.example.authservice.service.producer.VerificationCodeProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    VerificationCodeRepository verificationCodeRepository;
    @Mock
    JwtService jwtService;
    @Mock
    VerificationCodeProducer codeProducer;

    @InjectMocks
    AuthService authService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(authService, "codeExpirationMinutes", 10);
        ReflectionTestUtils.setField(authService, "codeSecret", "test-secret");
    }

    @Test
    void register_newUser_savesUserAndCode() {
        String email = "new@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(verificationCodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.register(email);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo(email);
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(Status.PENDING);

        verify(verificationCodeRepository).save(any(VerificationCode.class));
        verify(codeProducer).publish(eq(email), anyString());
    }

    @Test
    void register_existingUser_skipsUserCreation() {
        String email = "existing@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);
        when(verificationCodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.register(email);

        verify(userRepository, never()).save(any());
        verify(codeProducer).publish(eq(email), anyString());
    }

    @Test
    void register_generatesCode_sixDigits() {
        when(userRepository.existsByEmail(any())).thenReturn(true);
        when(verificationCodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        authService.register("user@test.com");

        verify(codeProducer).publish(any(), codeCaptor.capture());
        assertThat(codeCaptor.getValue()).matches("^[0-9]{6}$");
    }

    @Test
    void verify_validCode_returnsToken() {
        String email = "user@example.com";
        String rawCode = "123456";

        VerificationCode vc = VerificationCode.builder()
                .email(email)
                .codeHash(hashCode(rawCode))
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .attempts(0)
                .build();

        User user = User.builder()
                .email(email)
                .status(Status.PENDING)
                .build();

        when(verificationCodeRepository.findTopByEmailAndUsedFalseOrderByIdDesc(email))
                .thenReturn(Optional.of(vc));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(email)).thenReturn("jwt-token");
        when(verificationCodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String token = authService.verify(email, rawCode);

        assertThat(token).isEqualTo("jwt-token");
        assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(vc.isUsed()).isTrue();
    }

    @Test
    void verify_noCode_throwsNotFound() {
        when(verificationCodeRepository.findTopByEmailAndUsedFalseOrderByIdDesc(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verify("u@x.com", "000000"))
                .isInstanceOf(VerificationCodeNotFoundException.class);
    }

    @Test
    void verify_expiredCode_throwsExpired() {
        VerificationCode vc = VerificationCode.builder()
                .email("u@x.com")
                .codeHash(hashCode("111111"))
                .used(false)
                .expiresAt(LocalDateTime.now().minusMinutes(11))
                .attempts(0)
                .build();
        when(verificationCodeRepository.findTopByEmailAndUsedFalseOrderByIdDesc(any()))
                .thenReturn(Optional.of(vc));

        assertThatThrownBy(() -> authService.verify("u@x.com", "111111"))
                .isInstanceOf(VerificationCodeExpiredException.class);
    }

    @Test
    void verify_tooManyAttempts_throws() {
        VerificationCode vc = VerificationCode.builder()
                .email("u@x.com")
                .codeHash(hashCode("123456"))
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(11))
                .attempts(6)
                .build();
        when(verificationCodeRepository.findTopByEmailAndUsedFalseOrderByIdDesc(any()))
                .thenReturn(Optional.of(vc));

        assertThatThrownBy(() -> authService.verify("u@x.com", "123456"))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void verify_wrongCode_throwsInvalid() {
        VerificationCode vc = VerificationCode.builder()
                .email("u@x.com")
                .codeHash(hashCode("123456"))
                .expiresAt(LocalDateTime.now().plusMinutes(11))
                .used(false)
                .attempts(0)
                .build();
        when(verificationCodeRepository.findTopByEmailAndUsedFalseOrderByIdDesc(any()))
                .thenReturn(Optional.of(vc));

        assertThatThrownBy(() -> authService.verify("u@x.com", "111111"))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }

    @Test
    void verify_userNotFound_throws() {
        String email = "u@x.com";
        String rawCode = "123456";
        VerificationCode vc = VerificationCode.builder()
                .email("u@x.com")
                .codeHash(hashCode("123456"))
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(11))
                .attempts(0)
                .build();

        when(verificationCodeRepository.findTopByEmailAndUsedFalseOrderByIdDesc(email))
                .thenReturn(Optional.of(vc));
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(verificationCodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> authService.verify(email, rawCode))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getUsers_returnsAll() {
        User user = User.builder()
                .email("a@b.com")
                .status(Status.PENDING)
                .build();
        List<User> users = List.of(user);
        when(userRepository.findAll()).thenReturn(users);

        assertThat(authService.getUsers()).isEqualTo(users);
    }


    private String hashCode(String code) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((code + "test-secret").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}