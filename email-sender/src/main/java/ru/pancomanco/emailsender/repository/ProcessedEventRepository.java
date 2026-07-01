package ru.pancomanco.emailsender.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.pancomanco.emailsender.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
