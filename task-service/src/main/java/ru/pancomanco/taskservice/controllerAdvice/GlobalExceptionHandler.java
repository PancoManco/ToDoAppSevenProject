package ru.pancomanco.taskservice.controllerAdvice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.pancomanco.common.i18n.MessageService;
import ru.pancomanco.taskservice.exception.TaskNotFoundException;

import java.net.URI;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(TaskNotFoundException.class)
    public ProblemDetail handleNotFound(
            TaskNotFoundException exception
    ) {
        log.debug(
                "Task not found: taskId={}",
                exception.getTaskId()
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                messageService.get(
                        "task.not_found",
                        exception.getTaskId()
                )
        );
        problem.setTitle("Task not found");
        problem.setProperty(
                "taskId",
                exception.getTaskId()
        );

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String detail = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .filter(message -> !message.isBlank())
                .orElseGet(() ->
                        messageService.get("validation.failed")
                );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                detail
        );
        problem.setTitle("Validation failed");
        return problem;
    }
}
