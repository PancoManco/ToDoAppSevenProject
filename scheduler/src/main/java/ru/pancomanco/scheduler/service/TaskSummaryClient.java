package ru.pancomanco.scheduler.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.pancomanco.scheduler.dto.DailySummaryResponseDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskSummaryClient {
    private static final String INSTANCE = "taskService";

    private final RestClient taskServiceRestClient;

    @CircuitBreaker(name = INSTANCE, fallbackMethod = "fetchDailySummaryFallback")
    @Retry(name = INSTANCE)
    public DailySummaryResponseDto fetchDailySummary() {
        log.debug("Fetching daily summary from task-service");
        return taskServiceRestClient.get()
                .uri("/internal/tasks/daily-summary")
                .retrieve()
                .body(DailySummaryResponseDto.class);
    }


    private DailySummaryResponseDto fetchDailySummaryFallback(Throwable t) {
        log.error("task-service unavailable, skipping daily reports this run: {}", t.getMessage());
        return new DailySummaryResponseDto(List.of());
    }
}
