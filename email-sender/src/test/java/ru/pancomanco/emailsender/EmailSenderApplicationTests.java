package ru.pancomanco.emailsender;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.pancomanco.emailsender.config.TestMailConfig;
import ru.pancomanco.emailsender.config.TestcontainersConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestMailConfig.class})
class EmailSenderApplicationTests {

    @Test
    void contextLoads() {
    }
}
