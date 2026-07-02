package ru.pancomanco.emailsender.exception;

public class NonRetryableException extends RuntimeException {
    public NonRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
