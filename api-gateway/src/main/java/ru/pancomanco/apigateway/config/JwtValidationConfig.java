package ru.pancomanco.apigateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import ru.pancomanco.apigateway.properties.JwtProperties;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class JwtValidationConfig {


    private final JwtProperties jwtProperties;

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withJwkSetUri(jwtProperties.jwkSetUri())
                .build();

        OAuth2TokenValidator<Jwt> defaultValidators =
                JwtValidators.createDefaultWithIssuer(jwtProperties.issuer());

        OAuth2TokenValidator<Jwt> accessTypeValidator = tokenTypeValidator("access");

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(defaultValidators, accessTypeValidator)
        );

        return decoder;
    }

    private OAuth2TokenValidator<Jwt> tokenTypeValidator(String expectedType) {
        return jwt -> {
            String actualType = jwt.getClaimAsString("token_type");
            if (expectedType.equals(actualType)) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "Invalid token_type. Expected: " + expectedType,
                    null
            );
            return OAuth2TokenValidatorResult.failure(error);
        };
    }
}
