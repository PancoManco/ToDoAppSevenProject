package ru.pancomanco.authservice.service;

public interface PasswordResetService {

    void sendResetLink(String email);

    void resetPassword(String token, String newPassword);
}
