package ru.pancomanco.todoappsevenproject.controllerAdvice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.pancomanco.todoappsevenproject.dto.response.MessageResponseDto;
import ru.pancomanco.todoappsevenproject.exception.AppException;
import ru.pancomanco.todoappsevenproject.exception.RateLimitExceededException;
import ru.pancomanco.todoappsevenproject.exception.UnauthorizedException;
import ru.pancomanco.todoappsevenproject.service.MessageService;

import java.util.Map;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

//    @ExceptionHandler(UnauthorizedException.class)
//    ResponseEntity<?> unauthorized(UnauthorizedException ex) {
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                .body(Map.of("error", ex.getMessage()));
//    }

    @ExceptionHandler(JwtException.class)
    ResponseEntity<MessageResponseDto> jwtError(JwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponseDto("Invalid token"));
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    ResponseEntity<MessageResponseDto> missingCookie(MissingRequestCookieException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponseDto("Missing refresh token"));
    }


    @ExceptionHandler(AppException.class)
    public ResponseEntity<MessageResponseDto> handleAppException(AppException ex) {
        if (ex.getStatus().is4xxClientError()) {
            log.debug("Exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        } else {
            log.warn("Exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        }
        String message = messageService.get(ex.getErrorCode().getMessageKey());
        return ResponseEntity
                .status(ex.getStatus())
                .body(new MessageResponseDto(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponseDto> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        log.debug("Validation failed: {}", ex.getBindingResult().getAllErrors());
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(messageService.get("validation.failed"));
        return ResponseEntity
                .badRequest()
                .body(new MessageResponseDto(message));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<MessageResponseDto> handleRateLimitExceeded(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded. Retry after {} seconds", ex.getRetryAfterSeconds());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(new MessageResponseDto(messageService.get("auth.rate_limit.exceeded")));
    }
}
