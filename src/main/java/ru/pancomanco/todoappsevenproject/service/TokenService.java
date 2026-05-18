package ru.pancomanco.todoappsevenproject.service;

import org.springframework.security.core.Authentication;

public interface TokenService {
    public String generateToken(Authentication authentication);
}
