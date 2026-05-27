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

    public void sendPasswordResetLink(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(mailProperties.from());
        message.setTo(to);
        message.setSubject("Reset your password");
        message.setText("""
            You requested password reset.

            Open this link to set a new password:

            %s

            This link will expire in 15 minutes.

            If you did not request password reset, ignore this email.
            """.formatted(resetLink));

        mailSender.send(message);
    }

}
