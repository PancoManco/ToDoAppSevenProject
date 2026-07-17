package ru.pancomanco.authservice.exception;

public class PasswordResetException extends AppException {
    public PasswordResetException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PasswordResetException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
