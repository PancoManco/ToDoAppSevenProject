package ru.pancomanco.todoappsevenproject.exception;

public class PasswordResetException extends AppException {
    public PasswordResetException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PasswordResetException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
