package ru.pancomanco.emailsender.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.pancomanco.common.i18n.MessageService;
import ru.pancomanco.emailsender.properties.MailProperties;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class WelcomeEmailSender {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final MessageService messageService;

    public void sendWelcomeEmail(
            String to,
            String name
    ) {
        Locale locale = mailProperties.locale();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(to);

        message.setSubject(
                messageService.get(
                        locale,
                        "mail.welcome.subject"
                )
        );

        message.setText(
                messageService.get(
                        locale,
                        "mail.welcome.body",
                        resolveName(name)
                )
        );

        mailSender.send(message);

        log.info("Welcome email sent to {}", to);
    }

    private String resolveName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        return name;
    }
}
