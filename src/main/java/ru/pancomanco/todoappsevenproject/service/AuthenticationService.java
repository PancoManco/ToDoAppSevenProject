package ru.pancomanco.todoappsevenproject.service;

import ru.pancomanco.todoappsevenproject.dto.TokenPair;
import ru.pancomanco.todoappsevenproject.dto.request.LoginRequestDto;
import ru.pancomanco.todoappsevenproject.dto.request.RegisterRequestDto;
import ru.pancomanco.todoappsevenproject.dto.response.SignInResponseDto;
import ru.pancomanco.todoappsevenproject.dto.response.SignUpResponseDto;

public interface AuthenticationService {

    TokenPair register(RegisterRequestDto registerRequestDto);
    TokenPair login(LoginRequestDto loginRequestDto);
    TokenPair refresh(String refreshToken);
    void logout(String refreshToken);

}
