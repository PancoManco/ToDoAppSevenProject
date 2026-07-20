package ru.pancomanco.emailsender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.pancomanco.emailsender.properties.MailProperties;

@SpringBootApplication
@EnableConfigurationProperties(MailProperties.class)
public class EmailSenderApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmailSenderApplication.class, args);
    }

}
