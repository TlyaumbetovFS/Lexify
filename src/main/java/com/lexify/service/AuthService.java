package com.lexify.service;

import com.lexify.dto.AuthRequest;
import com.lexify.model.User;
import com.lexify.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwords;
    @Transactional
    public User register(AuthRequest request) {
        var email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes");
        }
        var user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(request.password()));
        return users.saveAndFlush(user);
    }

    @Transactional(readOnly = true)
    public User login(AuthRequest request) {
        var user = users.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(this::invalidCredentials);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwords.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return user;
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
