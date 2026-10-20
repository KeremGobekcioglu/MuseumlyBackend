package com.kg.museumly.backend.security;


import com.kg.museumly.backend.security.jwt.JwtConfig;
import com.kg.museumly.backend.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final Duration TTL = Duration.ofDays(30);

    private final JwtConfig jwtConfig = new JwtConfig();
    private final SecretKey key = jwtConfig.jwtSecretKey(randomBase64Secret());
    private final JwtEncoder encoder = jwtConfig.jwtEncoder(key);
    private final JwtDecoder decoder = jwtConfig.jwtDecoder(key);
    private final JwtService jwtService = new JwtService(encoder, TTL);

    @Test
    void issuedToken_decodesToSameUserId() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.issueToken(userId);
        Jwt decoded = decoder.decode(token);

        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
    }

    @Test
    void issuedToken_expiresExactlyTtlAfterIssue() {
        String token = jwtService.issueToken(UUID.randomUUID());
        Jwt decoded = decoder.decode(token);

        Duration lifetime = Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt());
        assertThat(lifetime).isEqualTo(TTL);
    }

    @Test
    void issuedToken_hasExpectedIssuer() {
        Jwt decoded = decoder.decode(jwtService.issueToken(UUID.randomUUID()));

        assertThat(decoded.getClaimAsString("iss")).isEqualTo("museumly-backend");
    }

    @Test
    void decoder_rejectsTokenSignedWithDifferentKey() {
        SecretKey otherKey = jwtConfig.jwtSecretKey(randomBase64Secret());
        JwtService otherService = new JwtService(jwtConfig.jwtEncoder(otherKey), TTL);

        String forgedToken = otherService.issueToken(UUID.randomUUID());

        assertThatThrownBy(() -> decoder.decode(forgedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void decoder_rejectsExpiredToken() {
        // the decoder allows 60s of clock skew by default, so expire well beyond that
        // the encoder refuses expiresAt <= issuedAt, so build a token that was issued in the past
        Instant issuedAt = Instant.now().minus(Duration.ofHours(1));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofMinutes(5)))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String expiredToken = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        assertThatThrownBy(() -> decoder.decode(expiredToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void decoder_rejectsTamperedPayload() {
        String tokenA = jwtService.issueToken(UUID.randomUUID());
        String tokenB = jwtService.issueToken(UUID.randomUUID());

        // header + payload of B, but the signature of A: the signature no longer matches
        String[] a = tokenA.split("\\.");
        String[] b = tokenB.split("\\.");
        String tampered = a[0] + "." + b[1] + "." + a[2];

        assertThatThrownBy(() -> decoder.decode(tampered))
                .isInstanceOf(JwtException.class);
    }

    private static String randomBase64Secret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
