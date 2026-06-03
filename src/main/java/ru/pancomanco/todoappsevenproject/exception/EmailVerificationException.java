package ru.pancomanco.todoappsevenproject.exception;

public class EmailVerificationException extends AppException {

    public EmailVerificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EmailVerificationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
