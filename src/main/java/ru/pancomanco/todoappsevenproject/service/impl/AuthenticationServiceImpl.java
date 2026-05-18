package ru.pancomanco.todoappsevenproject.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.todoappsevenproject.dto.request.SignInRequestDto;
import ru.pancomanco.todoappsevenproject.dto.request.SignUpRequestDto;
import ru.pancomanco.todoappsevenproject.dto.response.SignInResponseDto;
import ru.pancomanco.todoappsevenproject.dto.response.SignUpResponseDto;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.exception.UserAlreadyExistException;
import ru.pancomanco.todoappsevenproject.mapper.UserMapper;
import ru.pancomanco.todoappsevenproject.repository.AuthRepository;
import ru.pancomanco.todoappsevenproject.service.AuthenticationService;
import ru.pancomanco.todoappsevenproject.service.TokenService;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {


    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @Override
    @Transactional
    public SignUpResponseDto signUp(SignUpRequestDto signUpRequestDto) {
        Optional<User> existingUser = authRepository.findByUsername(signUpRequestDto.getUsername());
        if (existingUser.isPresent()) {
            throw new UserAlreadyExistException("error.user.already_exists");
        }
        String encodedPassword = passwordEncoder.encode(signUpRequestDto.getPassword());
        User user = userMapper.toEntity(signUpRequestDto);
        user.setPassword(encodedPassword);
        authRepository.save(user);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        String accessToken = tokenService.generateToken(authentication);
        return new SignUpResponseDto(user.getUsername(), accessToken);

    }

    @Override
    public SignInResponseDto signIn(SignInRequestDto signInRequestDto) {
        User user = userMapper.toEntityS(signInRequestDto);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        String accessToken = tokenService.generateToken(authentication);
        return new SignInResponseDto(user.getUsername(), accessToken);
    }
}
