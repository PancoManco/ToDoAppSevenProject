package ru.pancomanco.todoappsevenproject.service;

import org.springframework.security.core.Authentication;
import ru.pancomanco.todoappsevenproject.dto.TokenPair;
import ru.pancomanco.todoappsevenproject.entity.User;

public interface TokenService {
    TokenPair issueTokenPair(User user);
    TokenPair rotateRefreshTokenPair(String rawRefreshToken);
    void revokeRefreshTokenPair(String rawRefreshToken);
    String createAccessToken(User user);
    String createRefreshToken(User user);
    String sha256(String value);
}
