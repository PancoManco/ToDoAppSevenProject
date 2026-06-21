package ru.pancomanco.todoappsevenproject.service;

import ru.pancomanco.todoappsevenproject.entity.User;

import java.util.Map;


public interface SocialAuthService {
    User findOrCreateUser(String registrationId,
                          Map<String, Object> attributes);
}