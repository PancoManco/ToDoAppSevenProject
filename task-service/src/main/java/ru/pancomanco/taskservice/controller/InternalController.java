package ru.pancomanco.taskservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pancomanco.taskservice.dto.response.DailySummaryResponseDto;
import ru.pancomanco.taskservice.service.DailySummaryService;

@RestController
@RequestMapping("/internal/tasks")
@RequiredArgsConstructor
public class InternalController {

    private final DailySummaryService dailySummaryService;

    @GetMapping("/daily-summary")
    public DailySummaryResponseDto dailySummary() {
        return dailySummaryService.buildDailySummary();
    }
}
