package ru.pancomanco.scheduler.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ru.pancomanco.scheduler.dto.DailySummaryResponseDto;
import ru.pancomanco.scheduler.dto.DailySummaryResponseDto.UserTaskSummary;
import ru.pancomanco.scheduler.event.DailyReportEvent;
import ru.pancomanco.scheduler.kafka.ReportEventPublisher;
import ru.pancomanco.scheduler.service.TaskSummaryClient;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReportJob {

    private final TaskSummaryClient taskSummaryClient;
    private final ReportEventPublisher reportEventPublisher;

    @Scheduled(cron = "${app.report.cron}")
    public void generateDailyReports() {
        log.info("Starting daily report generation");

        DailySummaryResponseDto summary;
        try {
            summary = taskSummaryClient.fetchDailySummary();
        } catch (Exception e) {
            log.error("Failed to fetch daily summary from task-service", e);
            return;
        }

        if (summary == null || summary.users().isEmpty()) {
            log.info("No users with tasks to report");
            return;
        }

        int published = 0;
        for (UserTaskSummary user : summary.users()) {
            try {
                DailyReportEvent event = new DailyReportEvent(
                        UUID.randomUUID().toString(),
                        user.userId(),
                        user.email(),
                        user.name(),
                        user.completedCount(),
                        user.completedTitles(),
                        user.pendingCount(),
                        user.pendingTitles(),
                        Instant.now()
                );
                reportEventPublisher.publish(event);
                published++;
            } catch (Exception e) {
                log.error("Failed to publish report for user {}, skipping", user.userId(), e);
            }
        }

        log.info("Daily report generation finished: {} events published", published);
    }
}