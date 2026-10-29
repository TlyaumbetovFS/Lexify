package com.lexify.config;

import com.lexify.service.TokenService;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.assertj.core.api.Assertions.*;

class JwtTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void issuedTokenContainsOwnerAndExpiresInOneHour() {
        var key = config.jwtKey(secret((byte) 1));
        var response = new TokenService(config.jwtEncoder(key), Duration.ofHours(1)).create(42L);
        var jwt = config.jwtDecoder(key).decode(response.accessToken());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getIssuer().toString()).isEqualTo("https://lexify.local");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        var key = config.jwtKey(secret((byte) 1));
        var token = new TokenService(config.jwtEncoder(key), Duration.ofHours(1)).create(42L).accessToken();
        var otherKey = config.jwtKey(secret((byte) 2));
        assertThatThrownBy(() -> config.jwtDecoder(otherKey).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        var key = config.jwtKey(secret((byte) 1));
        var claims = JwtClaimsSet.builder().issuer("https://lexify.local").subject("42")
                .issuedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().minusSeconds(120)).build();
        var token = config.jwtEncoder(key).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        assertThatThrownBy(() -> config.jwtDecoder(key).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> config.jwtKey("c2hvcnQ=")).isInstanceOf(IllegalArgumentException.class);
    }

    private String secret(byte value) {
        var bytes = new byte[32];
        java.util.Arrays.fill(bytes, value);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
