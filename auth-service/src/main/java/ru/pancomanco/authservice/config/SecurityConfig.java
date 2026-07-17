package ru.pancomanco.authservice.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import ru.pancomanco.authservice.properties.AuthProperties;
import ru.pancomanco.authservice.properties.MailProperties;
import ru.pancomanco.authservice.properties.RateLimitProperties;
import ru.pancomanco.authservice.properties.RsaKeyProperties;
import ru.pancomanco.authservice.security.CookieEndpointOriginFilter;
import ru.pancomanco.authservice.security.OAuth2SuccessHandler;
import ru.pancomanco.authservice.util.RsaKeyLoader;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;


@Configuration
@EnableWebSecurity
@AllArgsConstructor
@EnableConfigurationProperties({AuthProperties.class, MailProperties.class, RateLimitProperties.class, RsaKeyProperties.class})
@Slf4j
public class SecurityConfig {

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "app.oauth2.enabled", havingValue = "true", matchIfMissing = false)
    SecurityFilterChain oauth2SecurityFilterChain(
            HttpSecurity http,
            OAuth2SuccessHandler successHandler,
            AuthProperties authProperties
    ) {
        return http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler)
                        .failureHandler((request, response, exception) -> {
                            log.warn("OAuth2 authentication failed . URI: {}, Error: {}, IP: {}",
                                    request.getRequestURI(), exception.getMessage(), request.getRemoteAddr());
                            response.sendRedirect(authProperties.oauth2FailureRedirect());
                        })
                )
                .build();
    }


    @Bean
   @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CookieEndpointOriginFilter originFilter,
                                           JwtAuthenticationConverter jwtAuthenticationConverter) {
        return http
                .csrf(csrf -> csrf.disable())
               // .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/.well-known/jwks.json").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(originFilter, BearerTokenAuthenticationFilter.class)
                .build();

    }


    @Bean
    JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        RSAKey jwk = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("auth-key-1")
                .build();

        JWKSource<SecurityContext> jwkSource =
                new ImmutableJWKSet<>(new JWKSet(jwk));

        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    @Primary
    JwtDecoder jwtDecoder(RSAPublicKey publicKey, AuthProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(properties.jwt().issuer());

        OAuth2TokenValidator<Jwt> tokenTypeValidator =
                tokenTypeValidator("access");

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(issuerValidator, tokenTypeValidator)
        );
        return decoder;
    }

    @Bean
    @Qualifier("refreshJwtDecoder")
    JwtDecoder refreshJwtDecoder(RSAPublicKey publicKey, AuthProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(properties.jwt().issuer());

        OAuth2TokenValidator<Jwt> tokenTypeValidator =
                tokenTypeValidator("refresh");

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(issuerValidator, tokenTypeValidator)
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

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter rolesConverter = new JwtGrantedAuthoritiesConverter();
        rolesConverter.setAuthoritiesClaimName("roles");
        rolesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(rolesConverter);

        return converter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }


    @Bean
    RSAPrivateKey rsaPrivateKey(RsaKeyProperties rsaKeyProperties) {
        return RsaKeyLoader.loadPrivateKey(rsaKeyProperties.privateKeyBase64());
    }

    @Bean
    RSAPublicKey rsaPublicKey(RsaKeyProperties rsaKeyProperties) {
        return RsaKeyLoader.loadPublicKey(rsaKeyProperties.publicKeyBase64());
    }
}
