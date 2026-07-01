package ru.pancomanco.emailsender.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestMailConfig {

    @Bean
    @Primary
    JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }
}
