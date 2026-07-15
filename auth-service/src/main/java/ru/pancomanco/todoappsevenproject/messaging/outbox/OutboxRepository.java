package ru.pancomanco.todoappsevenproject.messaging.outbox;

import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
            SELECT o.id FROM OutboxEvent o
            WHERE o.published = false
              AND o.dead = false
              AND o.nextAttemptAt <= :now
            ORDER BY o.createdAt ASC
            """)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<Long> findReadyToPublishIds(@Param("now") Instant now, Limit limit);

    @Modifying
    @Query("""
            DELETE FROM OutboxEvent o
            WHERE o.published = true AND o.createdAt < :cutoff
            """)
    int deletePublishedOlderThan(@Param("cutoff") Instant cutoff);
}
