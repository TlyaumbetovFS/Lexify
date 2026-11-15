package com.lexify.service;

import com.lexify.dto.TokenResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration ttl;

    public TokenService(JwtEncoder encoder, @Value("${app.jwt.ttl}") Duration ttl) {
        this.encoder = encoder;
        this.ttl = ttl;
    }

    public TokenResponse create(Long userId) {
        return create(userId, Instant.MAX);
    }

    public TokenResponse create(Long userId, Instant validUntil) {
        var now = Instant.now();
        var expiresAt = now.plus(ttl);
        if (validUntil.isBefore(expiresAt)) {
            expiresAt = validUntil;
        }
        var claims = JwtClaimsSet.builder()
                .issuer("https://lexify.local")
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = encoder.encode(JwtEncoderParameters.from(header, claims));
        return new TokenResponse(token.getTokenValue(), "Bearer", expiresAt.getEpochSecond() - now.getEpochSecond());
    }
}
