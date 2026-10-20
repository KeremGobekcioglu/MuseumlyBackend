package com.kg.museumly.backend.controller;

import com.kg.museumly.backend.dtos.AuthResponse;
import com.kg.museumly.backend.dtos.GoogleLoginRequest;
import com.kg.museumly.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    // /auth/google
    @PostMapping("/google")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request)
    {
        String accessToken = authService.loginWithGoogle(request.idToken());
        return new AuthResponse(accessToken);
    }
}
