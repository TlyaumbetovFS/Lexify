package com.lexify.service;

import com.lexify.dto.AuthRequest;
import com.lexify.dto.TokenResponse;
import com.lexify.model.User;
import com.lexify.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private TokenService tokens;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, passwords, tokens);
    }

    @Test
    void registersNormalizedEmailAndHashesPassword() {
        var response = new TokenResponse("token", "Bearer", 3600);
        when(users.saveAndFlush(any())).thenAnswer(invocation -> {
            var user = invocation.getArgument(0, User.class);
            user.setId(1L);
            return user;
        });
        when(tokens.create(1L)).thenReturn(response);

        assertThat(service.register(new AuthRequest(" User@Example.com ", "password123"))).isEqualTo(response);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwords.matches("password123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsDuplicateEmail() {
        when(users.existsByEmail("user@example.com")).thenReturn(true);

        assertStatus(409, () -> service.register(new AuthRequest("USER@example.com", "password123")));

        verify(users, never()).saveAndFlush(any());
        verifyNoInteractions(tokens);
    }

    @Test
    void rejectsPasswordExceedingBcryptByteLimit() {
        assertStatus(400, () -> service.register(new AuthRequest("user@example.com", "я".repeat(37))));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void logsInWithNormalizedEmail() {
        var user = new User();
        user.setId(1L);
        user.setPasswordHash(passwords.encode("password123"));
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        var response = new TokenResponse("token", "Bearer", 3600);
        when(tokens.create(1L)).thenReturn(response);

        assertThat(service.login(new AuthRequest("USER@example.com", "password123"))).isEqualTo(response);
    }

    @Test
    void rejectsWrongPassword() {
        var user = new User();
        user.setPasswordHash(passwords.encode("password123"));
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertStatus(401, () -> service.login(new AuthRequest("user@example.com", "incorrect")));
        verifyNoInteractions(tokens);
    }

    @Test
    void rejectsUnknownEmail() {
        assertStatus(401, () -> service.login(new AuthRequest("unknown@example.com", "password123")));
        verifyNoInteractions(tokens);
    }

    private void assertStatus(int status, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode().value()).isEqualTo(status));
    }
}
