package ru.pancomanco.todoappsevenproject.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.parser.HttpParser;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // AuthenticationService
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

    // VerificationService
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


    // PasswordResetService
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

    // SocialAuthService
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
        ),

    // TokenService
    AUTH_USER_NOT_FOUND("auth.user.not_found",
            HttpStatus.UNAUTHORIZED
    ),
    REFRESH_TOKEN_IS_MISSING("token.refresh_token.missing",
            HttpStatus.UNAUTHORIZED
    ),
    INVALID_REFRESH_TOKEN("token.invalid_refresh_token",
            HttpStatus.UNAUTHORIZED
    ),
    REFRESH_TOKEN_REUSE_DETECTED("token.refresh_token_reuse_detected",
            HttpStatus.FORBIDDEN
    ),
    REFRESH_TOKEN_EXPIRED("token.refresh_token_is_expired",
            HttpStatus.UNAUTHORIZED
    ),
    INVALID_REFRESH_TOKEN_SUBJECT("token.invalid_refresh_token_subject",
            HttpStatus.UNAUTHORIZED
    );

    private final String messageKey;
    private final HttpStatus status;
}
