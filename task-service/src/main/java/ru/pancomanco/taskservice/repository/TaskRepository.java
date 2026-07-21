package ru.pancomanco.taskservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.pancomanco.taskservice.dto.UserCountProjection;
import ru.pancomanco.taskservice.dto.UserTitleProjection;
import ru.pancomanco.taskservice.entity.Task;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {


    @Query("""
            SELECT t.ownerId AS ownerId, COUNT(t) AS count
            FROM Task t
            WHERE t.completed = true AND t.completedAt >= :since
            GROUP BY t.ownerId
            """)
    List<UserCountProjection> countCompletedPerUserSince(@Param("since") Instant since);

    @Query("""
            SELECT t.ownerId AS ownerId, COUNT(t) AS count
            FROM Task t
            WHERE t.completed = false
            GROUP BY t.ownerId
            """)
    List<UserCountProjection> countPendingPerUser();

    @Query(value = """
            SELECT owner_id AS ownerId, title AS title FROM (
                SELECT owner_id, title,
                       ROW_NUMBER() OVER (PARTITION BY owner_id ORDER BY completed_at DESC) AS rn
                FROM tasks
                WHERE completed = true AND completed_at >= :since
            ) ranked WHERE rn <= :maxTitles
            """, nativeQuery = true)
    List<UserTitleProjection> findCompletedTitlesPerUserSince(
            @Param("since") Instant since, @Param("maxTitles") int maxTitles);

    @Query(value = """
            SELECT owner_id AS ownerId, title AS title FROM (
                SELECT owner_id, title,
                       ROW_NUMBER() OVER (PARTITION BY owner_id ORDER BY created_at DESC) AS rn
                FROM tasks
                WHERE completed = false
            ) ranked WHERE rn <= :maxTitles
            """, nativeQuery = true)
    List<UserTitleProjection> findPendingTitlesPerUser(@Param("maxTitles") int maxTitles);


    List<Task> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);
}
