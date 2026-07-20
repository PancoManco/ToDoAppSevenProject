package ru.pancomanco.emailsender.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.pancomanco.common.i18n.MessageService;
import ru.pancomanco.emailsender.event.DailyReportEvent;
import ru.pancomanco.emailsender.properties.MailProperties;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyReportEmailSender {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final MessageService messageService;

    public void sendReport(DailyReportEvent event) {
        Locale locale = mailProperties.locale();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(event.email());
        message.setSubject(buildSubject(event, locale));
        message.setText(buildBody(event, locale));
        mailSender.send(message);
        log.info("Daily report sent to {}", event.email());
    }

    private String buildSubject(
            DailyReportEvent event,
            Locale locale
    ) {
        boolean hasCompleted =
                event.completedCount() > 0;

        boolean hasPending =
                event.pendingCount() > 0;

        if (hasCompleted && hasPending) {
            return messageService.get(
                    locale,
                    "mail.daily_report.subject.completed_and_pending"
            );
        }

        if (hasCompleted) {
            return messageService.get(
                    locale,
                    "mail.daily_report.subject.completed",
                    event.completedCount()
            );
        }

        return messageService.get(
                locale,
                "mail.daily_report.subject.pending",
                event.pendingCount()
        );
    }

    private String buildBody(
            DailyReportEvent event,
            Locale locale
    ) {
        StringBuilder body = new StringBuilder();

        body.append(
                messageService.get(
                        locale,
                        "mail.daily_report.greeting",
                        resolveName(event.name())
                )
        );

        body.append("\n\n");

        if (event.completedCount() > 0) {
            body.append(
                    messageService.get(
                            locale,
                            "mail.daily_report.completed",
                            event.completedCount()
                    )
            );

            body.append("\n");
            appendTitles(body, event.completedTitles());
            body.append("\n");
        }

        if (event.pendingCount() > 0) {
            body.append(
                    messageService.get(
                            locale,
                            "mail.daily_report.pending",
                            event.pendingCount()
                    )
            );

            body.append("\n");
            appendTitles(body, event.pendingTitles());
            body.append("\n");
        }

        return body.toString();
    }

    private String resolveName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        return name;
    }

    private void appendTitles(
            StringBuilder body,
            List<String> titles
    ) {
        if (titles == null) {
            return;
        }

        for (String title : titles) {
            body.append("  • ")
                    .append(title)
                    .append("\n");
        }
    }
}
