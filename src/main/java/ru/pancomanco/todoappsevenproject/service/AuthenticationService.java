package ru.pancomanco.todoappsevenproject.service;

import ru.pancomanco.todoappsevenproject.dto.request.SignInRequestDto;
import ru.pancomanco.todoappsevenproject.dto.request.SignUpRequestDto;
import ru.pancomanco.todoappsevenproject.dto.response.SignInResponseDto;
import ru.pancomanco.todoappsevenproject.dto.response.SignUpResponseDto;

public interface AuthenticationService {

    public SignUpResponseDto signUp(SignUpRequestDto signUpRequestDto);

    public SignInResponseDto signIn(SignInRequestDto signInRequestDto);
}
