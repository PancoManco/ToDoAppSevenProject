package ru.pancomanco.taskservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.pancomanco.taskservice.config.TestSecurityConfig;
import ru.pancomanco.taskservice.config.TestcontainersConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestSecurityConfig.class})
class TaskServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
