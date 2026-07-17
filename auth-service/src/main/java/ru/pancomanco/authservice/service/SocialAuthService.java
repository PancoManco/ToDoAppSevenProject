package ru.pancomanco.authservice.service;

import ru.pancomanco.authservice.entity.User;

import java.util.Map;


public interface SocialAuthService {
    User findOrCreateUser(String registrationId,
                          Map<String, Object> attributes);
}