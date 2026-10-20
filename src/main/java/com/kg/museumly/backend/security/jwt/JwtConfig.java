package com.kg.museumly.backend.security.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Wires up JWT signing and verification for the app's own session tokens.
 * <p>
 * Tokens are signed with HS256 (HMAC-SHA256), a symmetric algorithm: the same secret
 * both signs tokens (encoder) and verifies them (decoder). Anyone holding the secret
 * can forge tokens, so it must never be committed for production
 */
@Configuration
public class JwtConfig {

    /**
     * Builds the HMAC key from the {@code jwt.secret} property.
     * <p>
     * The property is a base64-encoded string (generate one with {@code openssl rand -base64 32}).
     * It is decoded to raw bytes here; invalid base64 fails fast at startup.
     * HS256 expects at least 32 bytes (256 bits) of key material.
     * "HmacSHA256" is the JCA algorithm name, not the JWT name ("HS256").
     */
    @Bean
    public SecretKey jwtSecretKey(@Value("${jwt.secret}") String secret)
    {
        byte[] bytes = Base64.getDecoder().decode(secret);
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /**
     * Creates (signs) JWTs, e.g. after a successful Google login.
     * {@link ImmutableSecret} exposes the symmetric key to Nimbus as a key source.
     */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey key)
    {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    /**
     * Verifies incoming JWTs (signature and expiry) and turns them into {@code Jwt} objects.
     * Pinning {@code MacAlgorithm.HS256} means tokens signed with any other algorithm are
     * rejected, which must match what the encoder produces.
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey key)
    {
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
