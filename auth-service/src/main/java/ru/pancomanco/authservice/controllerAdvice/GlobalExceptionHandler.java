package ru.pancomanco.authservice.controllerAdvice;

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
import ru.pancomanco.common.i18n.MessageService;
import ru.pancomanco.authservice.dto.response.MessageResponseDto;
import ru.pancomanco.authservice.exception.AppException;
import ru.pancomanco.authservice.exception.RateLimitExceededException;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(JwtException.class)
    ResponseEntity<MessageResponseDto> jwtError(JwtException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponseDto(
                        messageService.get("security.token.invalid")
                ));
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    ResponseEntity<MessageResponseDto> missingCookie(MissingRequestCookieException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponseDto(
                        messageService.get(
                                "security.refresh_token.missing"
                        )
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponseDto> handleUnexpectedException(
            Exception exception
    ) {
        log.error("Unexpected application error", exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponseDto(
                        messageService.get("common.internal_error")
                ));
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
