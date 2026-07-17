package ru.pancomanco.authservice.messaging.exception;

public class KafkaPublishException extends RuntimeException {
    public KafkaPublishException(String message,Throwable cause) {
        super(message,cause);
    }
}
