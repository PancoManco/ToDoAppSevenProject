package ru.pancomanco.authservice.service;

import ru.pancomanco.authservice.dto.TokenPair;
import ru.pancomanco.authservice.entity.User;

public interface TokenService {
    TokenPair issueTokenPair(User user);
    TokenPair rotateRefreshTokenPair(String rawRefreshToken);
    void revokeRefreshTokenPair(String rawRefreshToken);
    String createAccessToken(User user);
    String createRefreshToken(User user);
}
