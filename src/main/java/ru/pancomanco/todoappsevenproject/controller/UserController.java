package ru.pancomanco.todoappsevenproject.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequestMapping("/api/me")
@RestController
public class UserController {

    @GetMapping
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "userId", jwt.getSubject(),
                "email", jwt.getClaimAsString("email"),
                "roles", jwt.getClaimAsStringList("roles")
        );
    }
}
