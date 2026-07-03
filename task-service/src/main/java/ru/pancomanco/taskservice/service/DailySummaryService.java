package ru.pancomanco.taskservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.taskservice.dto.UserCountProjection;
import ru.pancomanco.taskservice.dto.UserTitleProjection;
import ru.pancomanco.taskservice.dto.response.DailySummaryResponseDto.UserTaskSummary;
import ru.pancomanco.taskservice.dto.response.DailySummaryResponseDto;
import ru.pancomanco.taskservice.entity.TaskUser;
import ru.pancomanco.taskservice.repository.TaskRepository;
import ru.pancomanco.taskservice.repository.TaskUserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailySummaryService {

    private static final int MAX_TITLES = 5;

    private final TaskUserRepository taskUserRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public DailySummaryResponseDto buildDailySummary() {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);

        Map<Long, Long> completedCounts = toCountMap(taskRepository.countCompletedPerUserSince(since));
        Map<Long, Long> pendingCounts = toCountMap(taskRepository.countPendingPerUser());
        Map<Long, List<String>> completedTitles = toTitleMap(
                taskRepository.findCompletedTitlesPerUserSince(since, MAX_TITLES));
        Map<Long, List<String>> pendingTitles = toTitleMap(
                taskRepository.findPendingTitlesPerUser(MAX_TITLES));

        Set<Long> activeUserIds = new HashSet<>(completedCounts.keySet());
        activeUserIds.addAll(pendingCounts.keySet());

        if (activeUserIds.isEmpty()) {
            return new DailySummaryResponseDto(List.of());
        }

        Map<Long, TaskUser> users = taskUserRepository.findAllById(activeUserIds).stream()
                .collect(Collectors.toMap(TaskUser::getUserId, Function.identity()));

        List<UserTaskSummary> result = new ArrayList<>();
        for (Long userId : activeUserIds) {
            TaskUser user = users.get(userId);
            if (user == null) {
                continue;
            }
            result.add(new UserTaskSummary(
                    userId,
                    user.getEmail(),
                    user.getName(),
                    completedCounts.getOrDefault(userId, 0L).intValue(),
                    completedTitles.getOrDefault(userId, List.of()),
                    pendingCounts.getOrDefault(userId, 0L).intValue(),
                    pendingTitles.getOrDefault(userId, List.of())
            ));
        }

        return new DailySummaryResponseDto(result);
    }

    private Map<Long, Long> toCountMap(List<UserCountProjection> rows) {
        return rows.stream().collect(Collectors.toMap(
                UserCountProjection::getOwnerId,
                UserCountProjection::getCount));
    }

    private Map<Long, List<String>> toTitleMap(List<UserTitleProjection> rows) {
        return rows.stream().collect(Collectors.groupingBy(
                UserTitleProjection::getOwnerId,
                Collectors.mapping(UserTitleProjection::getTitle, Collectors.toList())));
    }
}
