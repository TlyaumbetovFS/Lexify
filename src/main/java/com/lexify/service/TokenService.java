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
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("https://lexify.local")
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = encoder.encode(JwtEncoderParameters.from(header, claims));
        return new TokenResponse(token.getTokenValue(), "Bearer", ttl.toSeconds());
    }
}
