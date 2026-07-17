package ru.pancomanco.authservice.exception;


public class UnauthorizedException extends AppException{

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

}
