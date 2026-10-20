package com.kg.museumly.backend.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Duration ttl;


    public JwtService(JwtEncoder jwtEncoder, @Value("${jwt.ttl}") Duration ttl) {
        this.jwtEncoder = jwtEncoder;
        this.ttl = ttl;
    }

    public String issueToken(UUID userId)
    {
        Instant now = Instant.now();

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer("museumly-backend")
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header,claimsSet)).getTokenValue();
    }
}
