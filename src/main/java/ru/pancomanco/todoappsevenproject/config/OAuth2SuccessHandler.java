package ru.pancomanco.todoappsevenproject.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import ru.pancomanco.todoappsevenproject.dto.TokenPair;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.service.SocialAuthService;
import ru.pancomanco.todoappsevenproject.service.TokenService;
import ru.pancomanco.todoappsevenproject.util.RefreshCookieHelper;

import java.io.IOException;
import java.time.Duration;

@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final SocialAuthService socialAuthService;
    private final TokenService tokenService;


    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2AuthenticationToken oauthToken =
                (OAuth2AuthenticationToken) authentication;

        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oauthUser = oauthToken.getPrincipal();

        User user = socialAuthService.findOrCreateUser(
                registrationId,
                oauthUser.getAttributes()
        );

        TokenPair tokens = tokenService.issueTokenPair(user);

        ResponseCookie refreshCookie = RefreshCookieHelper.create(
                tokens.refreshToken(),
                Duration.ofDays(7)
        );

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());


        // access token НЕ кладём в query parameter.
        // Frontend после redirect вызывает POST /api/auth/refresh
        // и получает access token.
        response.sendRedirect("http://localhost:5173/oauth/success");
    }
}

