package ru.pancomanco.todoappsevenproject.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import ru.pancomanco.todoappsevenproject.dto.TokenPair;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.exception.AppException;
import ru.pancomanco.todoappsevenproject.properties.AuthProperties;
import ru.pancomanco.todoappsevenproject.service.SocialAuthService;
import ru.pancomanco.todoappsevenproject.service.TokenService;
import ru.pancomanco.todoappsevenproject.util.RefreshCookieHelper;

import java.io.IOException;
import java.time.Duration;

@RequiredArgsConstructor
@Component
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final SocialAuthService socialAuthService;
    private final TokenService tokenService;
    private final RefreshCookieHelper refreshCookieHelper;
    private final AuthProperties properties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        try {
            OAuth2AuthenticationToken oauthToken =
                    (OAuth2AuthenticationToken) authentication;

            String registrationId =
                    oauthToken.getAuthorizedClientRegistrationId();

            OAuth2User oauthUser = oauthToken.getPrincipal();

            User user = socialAuthService.findOrCreateUser(
                    registrationId,
                    oauthUser.getAttributes()
            );

            TokenPair tokens = tokenService.issueTokenPair(user);

            ResponseCookie refreshCookie = refreshCookieHelper.create(
                    tokens.refreshToken(),
                    Duration.ofDays(properties.jwt().refreshTokenDays())
            );

            response.addHeader(
                    HttpHeaders.SET_COOKIE,
                    refreshCookie.toString()
            );

            response.sendRedirect(properties.oauth2SuccessRedirect());

        } catch (AppException ex) {
            log.warn("OAuth2 login failed: {}", ex.getErrorCode(), ex);

            response.sendRedirect(properties.oauth2FailureRedirect());
        }


    }
}

