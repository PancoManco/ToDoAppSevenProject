package ru.pancomanco.todoappsevenproject.service;

public interface PasswordResetService {

    void sendResetLink(String email);

    void resetPassword(String token, String newPassword);
}
