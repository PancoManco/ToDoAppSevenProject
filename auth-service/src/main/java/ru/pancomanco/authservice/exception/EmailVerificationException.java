package ru.pancomanco.authservice.exception;

public class EmailVerificationException extends AppException {

    public EmailVerificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EmailVerificationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
