package ru.pancomanco.todoappsevenproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import ru.pancomanco.todoappsevenproject.properties.MailProperties;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
public class EmailSender {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(mailProperties.from());
        message.setTo(to);
        message.setSubject("Your verification code");
        message.setText("""
                Your verification code is: %s

                This code will expire in 10 minutes.
                """.formatted(code));

        mailSender.send(message);
    }
}
