package ru.pancomanco.todoappsevenproject.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.pancomanco.todoappsevenproject.dto.request.SignInRequestDto;
import ru.pancomanco.todoappsevenproject.dto.request.SignUpRequestDto;
import ru.pancomanco.todoappsevenproject.entity.User;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    User toEntity(SignUpRequestDto signUpRequestDto);
    User toEntityS(SignInRequestDto signInRequestDto);
}
