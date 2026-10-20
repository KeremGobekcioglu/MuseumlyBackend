package com.kg.museumly.backend.controller;

import com.kg.museumly.backend.dtos.UserDto;
import com.kg.museumly.backend.entity.User;
import com.kg.museumly.backend.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userService.getById(userId);
        return new UserDto(user.getId(), user.getEmail(), user.getCreatedAt());
    }
}