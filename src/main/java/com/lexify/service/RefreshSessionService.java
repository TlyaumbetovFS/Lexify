package com.lexify.service;

import com.lexify.dto.AuthResponse;
import com.lexify.model.RefreshSession;
import com.lexify.model.User;
import com.lexify.repository.RefreshSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RefreshSessionService {

    public static final Duration LIFETIME = Duration.ofDays(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshSessionRepository sessions;
    private final TokenService tokens;

    @Transactional
    public Login create(User user, String previousToken) {
        var now = Instant.now();
        sessions.deleteExpired(now);
        if (previousToken != null) {
            sessions.revoke(hash(previousToken));
        }
        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        var refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var expiresAt = now.plus(LIFETIME);
        sessions.save(new RefreshSession(hash(refreshToken), user, expiresAt));
        return new Login(response(user, expiresAt), refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null) {
            throw expired();
        }
        var session = sessions.findById(hash(refreshToken))
                .filter(value -> value.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(this::expired);
        return response(session.getUser(), session.getExpiresAt());
    }

    @Transactional
    public void revoke(String refreshToken) {
        if (refreshToken != null) {
            sessions.revoke(hash(refreshToken));
        }
    }

    private AuthResponse response(User user, Instant expiresAt) {
        var token = tokens.create(user.getId(), expiresAt);
        return new AuthResponse(token.accessToken(), token.tokenType(), token.expiresIn(), user.getId(), user.getEmail());
    }

    private ResponseStatusException expired() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired");
    }

    private String hash(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record Login(AuthResponse response, String refreshToken) {
    }
}
