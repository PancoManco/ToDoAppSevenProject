package ru.pancomanco.todoappsevenproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class SignUpResponseDto {
    private String username;
    private String accessToken;
}
