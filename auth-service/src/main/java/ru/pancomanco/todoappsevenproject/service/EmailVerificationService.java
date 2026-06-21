package ru.pancomanco.todoappsevenproject.service;

import ru.pancomanco.todoappsevenproject.dto.TokenPair;
import ru.pancomanco.todoappsevenproject.entity.User;

public interface EmailVerificationService {

    void sendVerificationCode(User user);

    TokenPair verifyEmail(String email, String code);

    void resendCode(String email);
}
