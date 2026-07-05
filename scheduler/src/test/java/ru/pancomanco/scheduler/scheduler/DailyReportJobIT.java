package ru.pancomanco.scheduler.scheduler;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import ru.pancomanco.scheduler.config.TestcontainersConfiguration;
import ru.pancomanco.scheduler.dto.DailySummaryResponseDto;
import ru.pancomanco.scheduler.job.DailyReportJob;
import ru.pancomanco.scheduler.service.TaskSummaryClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class DailyReportJobIT {

    @Autowired
    private DailyReportJob dailyReportJob;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ConfluentKafkaContainer kafkaContainer;

    @MockitoBean
    private TaskSummaryClient taskSummaryClient;

    @Test
    void generatesReports_PublishesEventPerUser() {
        DailySummaryResponseDto summary = new DailySummaryResponseDto(List.of(
                new DailySummaryResponseDto.UserTaskSummary(1L, "a@test.com", "UserA",
                        2, List.of("Done1", "Done2"), 1, List.of("Pending1")),
                new DailySummaryResponseDto.UserTaskSummary(2L, "b@test.com", "UserB",
                        0, List.of(), 3, List.of("P1", "P2", "P3"))
        ));
        when(taskSummaryClient.fetchDailySummary()).thenReturn(summary);

        dailyReportJob.generateDailyReports();

        try (Consumer<String, String> consumer = createTestConsumer()) {
            consumer.subscribe(List.of("report-events"));
            ConsumerRecords<String, String> records =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 2);

            assertThat(records.count()).isEqualTo(2);

            List<String> payloads = new java.util.ArrayList<>();
            records.forEach(r -> payloads.add(r.value()));
            assertThat(payloads).anyMatch(p -> p.contains("a@test.com"));
            assertThat(payloads).anyMatch(p -> p.contains("b@test.com"));
        }
    }

    @Test
    void emptySummary_PublishesNothing() {
        when(taskSummaryClient.fetchDailySummary())
                .thenReturn(new DailySummaryResponseDto(List.of()));

        dailyReportJob.generateDailyReports();

        try (Consumer<String, String> consumer = createTestConsumer()) {
            consumer.subscribe(List.of("report-events"));
            ConsumerRecords<String, String> records =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(3));
            assertThat(records.count()).isZero();
        }
    }

    private Consumer<String, String> createTestConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-report-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }
}