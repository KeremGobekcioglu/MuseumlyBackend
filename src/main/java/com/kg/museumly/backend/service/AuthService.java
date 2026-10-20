package com.kg.museumly.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kg.museumly.backend.entity.User;
import com.kg.museumly.backend.security.google.GoogleTokenVerifier;
import com.kg.museumly.backend.security.jwt.JwtService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthService(GoogleTokenVerifier googleTokenVerifier, UserService userService, JwtService jwtService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    public String loginWithGoogle(String idToken)
    {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);
        String googleSub = payload.getSubject();
        String email = payload.getEmail();

        User user = userService.findOrCreateByGoogleSub(googleSub,email);
        return jwtService.issueToken(user.getId());
    }
}
