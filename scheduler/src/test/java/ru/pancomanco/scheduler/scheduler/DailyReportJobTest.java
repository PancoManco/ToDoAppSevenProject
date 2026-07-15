package ru.pancomanco.scheduler.scheduler;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.pancomanco.scheduler.dto.DailySummaryResponseDto;
import ru.pancomanco.scheduler.event.DailyReportEvent;
import ru.pancomanco.scheduler.job.DailyReportJob;
import ru.pancomanco.scheduler.kafka.ReportEventPublisher;
import ru.pancomanco.scheduler.service.TaskSummaryClient;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyReportJobTest {

    @Mock
    private TaskSummaryClient taskSummaryClient;

    @Mock
    private ReportEventPublisher reportEventPublisher;

    private DailyReportJob job;

    @BeforeEach
    void setUp() {
        job = new DailyReportJob(
                taskSummaryClient,
                reportEventPublisher,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void generateDailyReports_WhenTwoUsers_PublishesTwoEvents() {
        DailySummaryResponseDto summary = new DailySummaryResponseDto(List.of(
                new DailySummaryResponseDto.UserTaskSummary(
                        100L, "a@test.com", "User A",
                        1, List.of("done"),
                        2, List.of("todo1", "todo2")
                ),
                new DailySummaryResponseDto.UserTaskSummary(
                        200L, "b@test.com", "User B",
                        0, List.of(),
                        1, List.of("todo")
                )
        ));

        when(taskSummaryClient.fetchDailySummary()).thenReturn(summary);

        job.generateDailyReports();

        verify(reportEventPublisher, times(2)).publish(any(DailyReportEvent.class));
    }

    @Test
    void generateDailyReports_WhenFirstPublishFails_ContinuesWithNextUser() {
        DailySummaryResponseDto summary = new DailySummaryResponseDto(List.of(
                new DailySummaryResponseDto.UserTaskSummary(
                        100L, "a@test.com", "User A",
                        1, List.of("done"),
                        0, List.of()
                ),
                new DailySummaryResponseDto.UserTaskSummary(
                        200L, "b@test.com", "User B",
                        0, List.of(),
                        1, List.of("todo")
                )
        ));

        when(taskSummaryClient.fetchDailySummary()).thenReturn(summary);

        doThrow(new RuntimeException("Kafka down"))
                .doNothing()
                .when(reportEventPublisher)
                .publish(any(DailyReportEvent.class));

        job.generateDailyReports();

        verify(reportEventPublisher, times(2)).publish(any(DailyReportEvent.class));
    }

    @Test
    void generateDailyReports_WhenNoUsers_DoesNotPublish() {
        when(taskSummaryClient.fetchDailySummary())
                .thenReturn(new DailySummaryResponseDto(List.of()));

        job.generateDailyReports();

        verifyNoInteractions(reportEventPublisher);
    }
}
