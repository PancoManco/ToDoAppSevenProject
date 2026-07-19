package ru.pancomanco.authservice.service;

import java.util.Locale;

public interface PasswordResetService {

    void sendResetLink(String email, Locale locale);

    void resetPassword(String token, String newPassword);
}
