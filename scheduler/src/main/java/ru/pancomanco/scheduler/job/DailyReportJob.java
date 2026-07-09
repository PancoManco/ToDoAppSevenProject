package ru.pancomanco.scheduler.job;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
@Slf4j
public class DailyReportJob {

    private final TaskSummaryClient taskSummaryClient;
    private final ReportEventPublisher reportEventPublisher;

    private final Counter jobRuns;
    private final Counter reportsPublished;
    private final Counter publishErrors;

    public DailyReportJob(TaskSummaryClient taskSummaryClient,
                          ReportEventPublisher reportEventPublisher,
                          MeterRegistry meterRegistry) {
        this.taskSummaryClient = taskSummaryClient;
        this.reportEventPublisher = reportEventPublisher;

        this.jobRuns = Counter.builder("scheduler.job.runs")
                .description("Number of daily report job executions")
                .register(meterRegistry);

        this.reportsPublished = Counter.builder("scheduler.reports.published")
                .description("Number of report events published to Kafka")
                .register(meterRegistry);

        this.publishErrors = Counter.builder("scheduler.reports.publish.errors")
                .description("Number of failed report publications")
                .register(meterRegistry);
    }

    @Scheduled(cron = "${app.report.cron}")
    public void generateDailyReports() {
        log.info("Starting daily report generation");
        jobRuns.increment();

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
                reportsPublished.increment();
            } catch (Exception e) {
                log.error("Failed to publish report for user {}, skipping", user.userId(), e);
                publishErrors.increment();
            }
        }

        log.info("Daily report generation finished: {} events published", (long) reportsPublished.count());
    }
}