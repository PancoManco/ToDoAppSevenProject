package ru.pancomanco.todoappsevenproject.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends AppException{

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UnauthorizedException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
