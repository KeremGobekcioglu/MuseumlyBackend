package com.kg.museumly.backend.security.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class GoogleAuthConfig {

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(
            @Value("${google.oauth.client-id}") String clientId
    )
    {
        return new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Bean
    public GoogleTokenVerificationClient googleTokenVerificationClient(GoogleIdTokenVerifier verifier) {
        return new GoogleTokenVerificationClientImpl(verifier);
    }

    @Bean
    public GoogleTokenVerifier googleTokenVerifier(GoogleTokenVerificationClient client) {
        return new GoogleTokenVerifier(client);
    }
}
