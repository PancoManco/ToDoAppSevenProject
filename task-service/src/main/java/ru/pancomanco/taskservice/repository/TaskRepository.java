package ru.pancomanco.taskservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.pancomanco.taskservice.entity.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);
}
