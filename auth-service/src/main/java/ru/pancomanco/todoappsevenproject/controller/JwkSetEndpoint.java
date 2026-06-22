package ru.pancomanco.todoappsevenproject.controller;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@RestController
public class JwkSetEndpoint {

    private final RSAPublicKey rsaPublicKey;

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String,Object>> jwks() {
        log.debug("Jwk Set requested");

        RSAKey rsaKey = new RSAKey.Builder(rsaPublicKey)
                .keyID("auth-key-1")
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);

        Map<String,Object> jwkSetJson = jwkSet.toJSONObject();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(jwkSetJson);
    }
}
