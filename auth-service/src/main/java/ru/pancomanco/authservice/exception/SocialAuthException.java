package ru.pancomanco.authservice.exception;

public class SocialAuthException extends AppException {

    public SocialAuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SocialAuthException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
