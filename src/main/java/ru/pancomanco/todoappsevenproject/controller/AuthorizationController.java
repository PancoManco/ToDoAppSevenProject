package ru.pancomanco.todoappsevenproject.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pancomanco.todoappsevenproject.dto.BaseApiResponse;
import ru.pancomanco.todoappsevenproject.dto.request.SignInRequestDto;
import ru.pancomanco.todoappsevenproject.dto.request.SignUpRequestDto;
import ru.pancomanco.todoappsevenproject.dto.response.SignInResponseDto;
import ru.pancomanco.todoappsevenproject.dto.response.SignUpResponseDto;
import ru.pancomanco.todoappsevenproject.service.impl.AuthenticationServiceImpl;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthenticationServiceImpl authenticationService;

    @PostMapping("/sign-up")
    public ResponseEntity<BaseApiResponse<SignUpResponseDto>> signUp(@RequestBody SignUpRequestDto user) {
        SignUpResponseDto result = authenticationService.signUp(user);
        BaseApiResponse<SignUpResponseDto> signUpResponseDto = new BaseApiResponse<>();
        signUpResponseDto.setData(result);
        signUpResponseDto.setStatusCode(HttpStatus.CREATED.value());
        return new ResponseEntity<>(signUpResponseDto, HttpStatus.CREATED);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<BaseApiResponse<SignInResponseDto>> signIn(@RequestBody SignInRequestDto user) {
        SignInResponseDto result = authenticationService.signIn(user);
        BaseApiResponse<SignInResponseDto> signInResponseDto = new BaseApiResponse<>();
        signInResponseDto.setData(result);
        signInResponseDto.setStatusCode(HttpStatus.OK.value());
        return new ResponseEntity<>(signInResponseDto, HttpStatus.OK);
    }

//    @PostMapping("/sign-out")
//    public ResponseEntity<Void> signOut() {
//
//    }
}
