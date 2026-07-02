package ru.pancomanco.emailsender.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.pancomanco.emailsender.event.DailyReportEvent;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyReportEmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public void sendReport(DailyReportEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(event.email());
        message.setSubject(buildSubject(event));
        message.setText(buildBody(event));
        mailSender.send(message);
        log.info("Daily report sent to {}", event.email());
    }

    private String buildSubject(DailyReportEvent event) {
        boolean hasCompleted = event.completedCount() > 0;
        boolean hasPending = event.pendingCount() > 0;

        if (hasCompleted && hasPending) {
            return "Ваш отчёт за день: выполнено и осталось";
        } else if (hasCompleted) {
            return "За сегодня вы выполнили " + event.completedCount() + " задач";
        } else {
            return "У вас осталось " + event.pendingCount() + " несделанных задач";
        }
    }

    private String buildBody(DailyReportEvent event) {
        StringBuilder body = new StringBuilder();
        body.append("Здравствуйте, ").append(event.name()).append("!\n\n");

        if (event.completedCount() > 0) {
            body.append("За сегодня вы выполнили ")
                    .append(event.completedCount())
                    .append(" задач:\n");
            appendTitles(body, event.completedTitles());
            body.append("\n");
        }

        if (event.pendingCount() > 0) {
            body.append("У вас осталось ")
                    .append(event.pendingCount())
                    .append(" несделанных задач:\n");
            appendTitles(body, event.pendingTitles());
            body.append("\n");
        }

        return body.toString();
    }

    private void appendTitles(StringBuilder body, List<String> titles) {
        for (String title : titles) {
            body.append("  • ").append(title).append("\n");
        }
    }
}
