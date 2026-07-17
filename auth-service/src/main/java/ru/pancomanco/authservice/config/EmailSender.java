package ru.pancomanco.authservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import ru.pancomanco.common.i18n.MessageService;
import ru.pancomanco.authservice.properties.MailProperties;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
public class EmailSender {

    private static final int VERIFICATION_CODE_TTL_MINUTES = 5;
    private static final int PASSWORD_RESET_LINK_TTL_MINUTES = 15;

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final MessageService messageService;

    public void sendVerificationCode(String to, String code) {
        sendSimpleMessage(
                to,
                messageService.get("mail.verification.subject"),
                messageService.get(
                        "mail.verification.body",
                        code,
                        VERIFICATION_CODE_TTL_MINUTES
                )
        );
    }

    public void sendPasswordResetLink(String to, String resetLink) {
        sendSimpleMessage(
                to,
                messageService.get("mail.password-reset.subject"),
                messageService.get(
                        "mail.password-reset.body",
                        resetLink,
                        PASSWORD_RESET_LINK_TTL_MINUTES
                )
        );
    }

    private void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(mailProperties.from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

}
