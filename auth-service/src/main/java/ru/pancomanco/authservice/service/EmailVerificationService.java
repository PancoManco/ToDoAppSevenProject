package ru.pancomanco.authservice.service;

import ru.pancomanco.authservice.dto.TokenPair;
import ru.pancomanco.authservice.entity.User;

import java.util.Locale;

public interface EmailVerificationService {

    void sendVerificationCode(User user, Locale locale);

    TokenPair verifyEmail(String email, String code);

    void resendCode(String email, Locale locale);
}
