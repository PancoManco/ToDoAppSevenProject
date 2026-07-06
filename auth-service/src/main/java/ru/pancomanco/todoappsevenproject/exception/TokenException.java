package ru.pancomanco.todoappsevenproject.exception;

public class TokenException extends AppException{
    public TokenException(ErrorCode errorCode) {
        super(errorCode);
    }


}
