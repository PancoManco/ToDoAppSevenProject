package ru.pancomanco.todoappsevenproject.dto;

import lombok.Setter;

@Setter
public class BaseApiResponse<T> {
    private int statusCode;
    private T data;
}
