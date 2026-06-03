package ru.pancomanco.todoappsevenproject.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    AUTH_EMAIL_ALREADY_EXISTS(
            "auth.email.already_exists",
            HttpStatus.CONFLICT
    ),

    AUTH_INVALID_CREDENTIALS(
            "auth.invalid_credentials",
            HttpStatus.UNAUTHORIZED
    ),

    AUTH_EMAIL_NOT_VERIFIED(
            "auth.email.not_verified",
            HttpStatus.FORBIDDEN
    ),

    AUTH_VERIFICATION_CODE_INVALID(
            "auth.verification.code_invalid",
            HttpStatus.BAD_REQUEST
    ),

    AUTH_VERIFICATION_CODE_EXPIRED(
            "auth.verification.code_expired",
            HttpStatus.BAD_REQUEST
    ),

    AUTH_VERIFICATION_ATTEMPTS_EXCEEDED(
            "auth.verification.attempts_exceeded",
            HttpStatus.TOO_MANY_REQUESTS
    ),

    AUTH_EMAIL_ALREADY_VERIFIED(
            "auth.email.already_verified",
            HttpStatus.CONFLICT
    ),

    AUTH_VERIFICATION_EMAIL_SEND_FAILED(
            "auth.verification.email_send_failed",
            HttpStatus.SERVICE_UNAVAILABLE
    ),

    AUTH_PASSWORD_RESET_TOKEN_INVALID(
        "auth.password_reset.token_invalid",
        HttpStatus.BAD_REQUEST
        ),

    AUTH_PASSWORD_RESET_TOKEN_EXPIRED(
        "auth.password_reset.token_expired",
        HttpStatus.BAD_REQUEST
        ),

    AUTH_PASSWORD_RESET_EMAIL_SEND_FAILED(
        "auth.password_reset.email_send_failed",
        HttpStatus.SERVICE_UNAVAILABLE
        ),

    AUTH_SOCIAL_PROVIDER_UNSUPPORTED(
        "auth.social.provider_unsupported",
        HttpStatus.BAD_REQUEST
        ),

    AUTH_SOCIAL_PROFILE_INVALID(
        "auth.social.profile_invalid",
        HttpStatus.BAD_REQUEST
        ),

    AUTH_SOCIAL_EMAIL_MISSING(
        "auth.social.email_missing",
        HttpStatus.BAD_REQUEST
        ),

    AUTH_SOCIAL_EMAIL_NOT_VERIFIED(
        "auth.social.email_not_verified",
        HttpStatus.BAD_REQUEST
        );

    private final String messageKey;
    private final HttpStatus status;
}
