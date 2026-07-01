package ru.pancomanco.emailsender.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WelcomeEmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public void sendWelcomeEmail(String to, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Welcome to ToDo App!");
        message.setText(buildBody(name));
        mailSender.send(message);
        log.info("Welcome email sent to {}", to);
    }

    private String buildBody(String name) {
        return """
                WELCOME TEXTTTTTTTTTTTTTTTTT
                fdsafdsffffffdssssssss
                """.formatted(name);
    }
}
