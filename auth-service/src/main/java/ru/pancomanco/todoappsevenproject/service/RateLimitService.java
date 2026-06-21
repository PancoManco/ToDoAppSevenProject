package ru.pancomanco.todoappsevenproject.service;

public interface RateLimitService {
    void checkRegister(String ip, String email);

    void checkLogin(String ip, String email);

    void checkVerifyEmail(String ip, String email);

    void checkResendVerification(String ip, String email);

    void checkForgotPassword(String ip, String email);

    void checkResetPassword(String ip, String token);
}
