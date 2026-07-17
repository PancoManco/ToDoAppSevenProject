package ru.pancomanco.authservice.exception;

public class TokenException extends AppException{
    public TokenException(ErrorCode errorCode) {
        super(errorCode);
    }


}
