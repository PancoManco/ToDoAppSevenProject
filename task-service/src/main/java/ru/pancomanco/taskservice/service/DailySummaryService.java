package ru.pancomanco.taskservice.service;

import lombok.RequiredArgsConstructor;


import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.pancomanco.taskservice.dto.response.DailySummaryResponseDto.UserTaskSummary;
import ru.pancomanco.taskservice.dto.response.DailySummaryResponseDto;
import ru.pancomanco.taskservice.entity.TaskUser;
import ru.pancomanco.taskservice.repository.TaskRepository;
import ru.pancomanco.taskservice.repository.TaskUserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailySummaryService {

    private static final int MAX_TITLES = 5;

    private final TaskUserRepository taskUserRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public DailySummaryResponseDto buildDailySummary() {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        List<UserTaskSummary> result = new ArrayList<>();

        for (TaskUser user : taskUserRepository.findAll()) {
            Long userId = user.getUserId();

            int completedCount = taskRepository.countCompletedSince(userId, since);
            int pendingCount = taskRepository.countByOwnerIdAndCompletedFalse(userId);

            if (completedCount == 0 && pendingCount == 0) {
                continue;
            }

            List<String> completedTitles = completedCount > 0
                    ? taskRepository.findCompletedTitlesSince(userId, since, PageRequest.of(0,MAX_TITLES))
                    : List.of();

            List<String> pendingTitles = pendingCount > 0
                    ? taskRepository.findPendingTitles(userId, PageRequest.of(0,MAX_TITLES))
                    : List.of();

            result.add(new UserTaskSummary(
                    userId,
                    user.getEmail(),
                    user.getName(),
                    completedCount,
                    completedTitles,
                    pendingCount,
                    pendingTitles
            ));
        }

        return new DailySummaryResponseDto(result);
    }
}
