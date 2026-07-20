package ru.pancomanco.taskservice.exception;

import lombok.Getter;

@Getter
public class TaskNotFoundException extends RuntimeException {
  private final Long taskId;

  public TaskNotFoundException(Long taskId) {
    super("Task not found, id=" + taskId);
    this.taskId = taskId;
  }
}
