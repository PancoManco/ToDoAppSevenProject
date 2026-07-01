package ru.pancomanco.todoappsevenproject.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.todoappsevenproject.dto.TokenPair;
import ru.pancomanco.todoappsevenproject.entity.RefreshToken;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.exception.ErrorCode;
import ru.pancomanco.todoappsevenproject.exception.TokenException;
import ru.pancomanco.todoappsevenproject.exception.UnauthorizedException;
import ru.pancomanco.todoappsevenproject.properties.AuthProperties;
import ru.pancomanco.todoappsevenproject.repository.AuthRepository;
import ru.pancomanco.todoappsevenproject.repository.RefreshTokenRepository;
import ru.pancomanco.todoappsevenproject.service.TokenService;
import ru.pancomanco.todoappsevenproject.util.HashUtil;

import java.time.Duration;
import java.time.Instant;
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
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_USER_NOT_FOUND));

        log.debug("Issuing new token pair for user ID: {}. Revoking previous active sessions.", managedUser.getId());
        refreshTokenRepository.revokeAllActiveTokensByUserId(managedUser.getId());
        String accessToken = createAccessToken(managedUser);
        String refreshToken = createRefreshToken(managedUser);

        String refreshTokenHash = HashUtil.sha256Hex(refreshToken);
        Instant refreshExpiresAt = Instant.now()
                .plus(Duration.ofDays(properties.jwt().refreshTokenDays()));

        refreshTokenRepository.save(
                new RefreshToken(managedUser, refreshTokenHash, refreshExpiresAt)
        );

        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    @Transactional(noRollbackFor = TokenException.class)
    public TokenPair rotateRefreshTokenPair(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new TokenException(ErrorCode.REFRESH_TOKEN_IS_MISSING);
        }

        Jwt jwt;
        try {
            jwt = refreshJwtDecoder.decode(rawRefreshToken);
        } catch (JwtException ex) {
            throw new TokenException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String tokenHash = HashUtil.sha256Hex(rawRefreshToken);

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() ->  new TokenException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (currentToken.isRevoked()) {
            log.warn("SECURITY ALERT: Refresh token reuse detected for user ID: {}. All sessions terminated.",
                    currentToken.getUser().getId());
            refreshTokenRepository.revokeAllActiveTokensByUserId(currentToken.getUser().getId());
            throw new TokenException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        }

        if (currentToken.isExpired()) {
            log.debug("Expired refresh token used for user ID: {}", currentToken.getUser().getId());
            currentToken.revoke();
            throw new TokenException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        if (!jwt.getSubject().equals(String.valueOf(currentToken.getUser().getId()))) {
            log.error("SECURITY ALERT: Refresh token subject mismatch! JWT subject: {}, DB user ID: {}",
                    jwt.getSubject(), currentToken.getUser().getId());
            refreshTokenRepository.revokeAllActiveTokensByUserId(currentToken.getUser().getId());
            throw new TokenException(ErrorCode.INVALID_REFRESH_TOKEN_SUBJECT);
        }
        log.debug("Successfully rotated refresh token for user ID: {}", currentToken.getUser().getId());
        currentToken.revoke();
        return issueTokenPair(currentToken.getUser());

    }

    @Transactional
    @Override
    public void revokeRefreshTokenPair(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String tokenHash = HashUtil.sha256Hex(rawRefreshToken);
//        refreshTokenRepository
//                .findByTokenHashAndRevokedFalse(tokenHash)
//                .ifPresent(RefreshToken::revoke);
//
//        String tokenHash = sha256(rawRefreshToken);
        int updatedRows = refreshTokenRepository.revokeByTokenHashIfActive(tokenHash);
        if (updatedRows > 0) {
            log.debug("Successfully revoked refresh token (hash matched).");
        } else {
            log.debug("Attempted to revoke refresh token, but it was already revoked or not found.");
        }
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
                .claim("name", user.getName())  // toCheck
                .claim("roles", List.of(user.getRole().name()))
                .build();

        JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256).build();
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

        JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256).build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(headers, claims))
                .getTokenValue();

    }

}
