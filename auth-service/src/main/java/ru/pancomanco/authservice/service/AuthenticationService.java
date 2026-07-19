package ru.pancomanco.authservice.service;

import ru.pancomanco.authservice.dto.TokenPair;
import ru.pancomanco.authservice.dto.request.LoginRequestDto;
import ru.pancomanco.authservice.dto.request.RegisterRequestDto;

import java.util.Locale;

public interface AuthenticationService {

    void register(RegisterRequestDto registerRequestDto, Locale locale);

    TokenPair login(LoginRequestDto loginRequestDto);

    TokenPair refresh(String refreshToken);

    void logout(String refreshToken);

}
