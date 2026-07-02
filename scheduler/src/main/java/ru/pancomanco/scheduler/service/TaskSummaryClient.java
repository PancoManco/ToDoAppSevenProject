package ru.pancomanco.scheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.pancomanco.scheduler.dto.DailySummaryResponseDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskSummaryClient {

    private final RestClient taskServiceRestClient;

    public DailySummaryResponseDto fetchDailySummary() {
        log.debug("Fetching daily summary from task-service");
        return taskServiceRestClient.get()
                .uri("/internal/tasks/daily-summary")
                .retrieve()
                .body(DailySummaryResponseDto.class);
    }
}
