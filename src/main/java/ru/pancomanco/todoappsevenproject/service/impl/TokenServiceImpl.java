package ru.pancomanco.todoappsevenproject.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.todoappsevenproject.dto.TokenPair;
import ru.pancomanco.todoappsevenproject.entity.RefreshToken;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.exception.UnauthorizedException;
import ru.pancomanco.todoappsevenproject.properties.AuthProperties;
import ru.pancomanco.todoappsevenproject.repository.AuthRepository;
import ru.pancomanco.todoappsevenproject.repository.RefreshTokenRepository;
import ru.pancomanco.todoappsevenproject.service.TokenService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TokenServiceImpl implements TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder refreshJwtDecoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthRepository authRepository;
    private final AuthProperties properties;

    public TokenServiceImpl(
            JwtEncoder jwtEncoder,
            @Qualifier("refreshJwtDecoder") JwtDecoder refreshJwtDecoder,
            RefreshTokenRepository refreshTokenRepository,
            AuthRepository userRepository,
            AuthProperties properties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.refreshJwtDecoder = refreshJwtDecoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authRepository = userRepository;
        this.properties = properties;
    }


    @Transactional
    @Override
    public TokenPair issueTokenPair(User user) {

        User managedUser = authRepository.findById(user.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        refreshTokenRepository.revokeAllActiveTokensByUserId(managedUser.getId());
        String accessToken = createAccessToken(managedUser);
        String refreshToken = createRefreshToken(managedUser);

        String refreshTokenHash = sha256(refreshToken);
        Instant refreshExpiresAt = Instant.now()
                .plus(Duration.ofDays(properties.jwt().refreshTokenDays()));

        refreshTokenRepository.save(
                new RefreshToken(managedUser, refreshTokenHash, refreshExpiresAt)
        );

        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenPair rotateRefreshTokenPair(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Missing refresh token");
        }

        Jwt jwt;
        try {
            jwt = refreshJwtDecoder.decode(rawRefreshToken);
        } catch (JwtException ex) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String tokenHash = sha256(rawRefreshToken);

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (currentToken.isRevoked()) {
            refreshTokenRepository.revokeAllActiveTokensByUserId(currentToken.getUser().getId());
            throw new UnauthorizedException("Refresh token reuse detected");
        }

        if (currentToken.isExpired()) {
            currentToken.revoke();
            throw new UnauthorizedException("Refresh token expired");
        }

        if (!jwt.getSubject().equals(String.valueOf(currentToken.getUser().getId()))) {
            refreshTokenRepository.revokeAllActiveTokensByUserId(currentToken.getUser().getId());
            throw new UnauthorizedException("Invalid refresh token subject");
        }
        currentToken.revoke();

        return issueTokenPair(currentToken.getUser());

    }

    @Transactional
    @Override
    public void revokeRefreshTokenPair(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String tokenHash = sha256(rawRefreshToken);
        refreshTokenRepository
                .findByTokenHashAndRevokedFalse(tokenHash)
                .ifPresent(RefreshToken::revoke);
    }

    @Override
    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(properties.jwt().accessTokenMinutes()));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .claim("token_type", "access")
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole().name()))
                .build();

        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder
                .encode(JwtEncoderParameters.from(headers, claims))
                .getTokenValue();
    }

    @Override
    public String createRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofDays(properties.jwt().refreshTokenDays()));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getId()))
                .claim("token_type", "refresh")
                .build();

        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(headers, claims))
                .getTokenValue();

    }

    @Override
    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash token", e);
        }
    }
}
