package ru.pancomanco.authservice.service;

import ru.pancomanco.authservice.dto.TokenPair;
import ru.pancomanco.authservice.entity.User;

public interface EmailVerificationService {

    void sendVerificationCode(User user);

    TokenPair verifyEmail(String email, String code);

    void resendCode(String email);
}
