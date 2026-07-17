package ru.pancomanco.authservice.service;

import ru.pancomanco.authservice.dto.TokenPair;
import ru.pancomanco.authservice.dto.request.LoginRequestDto;
import ru.pancomanco.authservice.dto.request.RegisterRequestDto;

public interface AuthenticationService {

    void register(RegisterRequestDto registerRequestDto);

    TokenPair login(LoginRequestDto loginRequestDto);

    TokenPair refresh(String refreshToken);

    void logout(String refreshToken);

}
