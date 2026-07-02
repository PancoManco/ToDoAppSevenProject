package ru.pancomanco.taskservice.dto;

public interface UserCountProjection {
    Long getOwnerId();
    long getCount();
}
