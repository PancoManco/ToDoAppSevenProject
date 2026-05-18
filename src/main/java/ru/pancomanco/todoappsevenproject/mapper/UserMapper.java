package ru.pancomanco.todoappsevenproject.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.pancomanco.todoappsevenproject.dto.request.LoginRequestDto;
import ru.pancomanco.todoappsevenproject.dto.request.RegisterRequestDto;
import ru.pancomanco.todoappsevenproject.entity.User;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    User toEntity(RegisterRequestDto signUpRequestDto);
    User toEntityS(LoginRequestDto signInRequestDto);
}
