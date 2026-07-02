package ru.pancomanco.taskservice.repository;

import org.hibernate.query.spi.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.pancomanco.taskservice.entity.Task;


import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
        SELECT t.title FROM Task t
        WHERE t.ownerId = :ownerId
          AND t.completed = true
          AND t.completedAt >= :since
        ORDER BY t.completedAt DESC
        """)
    List<String> findCompletedTitlesSince(Long ownerId, Instant since, Pageable pageable);

    @Query("""
        SELECT COUNT(t) FROM Task t
        WHERE t.ownerId = :ownerId
          AND t.completed = true
          AND t.completedAt >= :since
        """)
    int countCompletedSince(Long ownerId, Instant since);

    @Query("""
        SELECT t.title FROM Task t
        WHERE t.ownerId = :ownerId
          AND t.completed = false
        ORDER BY t.createdAt DESC
        """)
    List<String> findPendingTitles(Long ownerId, org.springframework.data.domain.Pageable pageable);

    int countByOwnerIdAndCompletedFalse(Long ownerId);

    List<Task> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);
}
