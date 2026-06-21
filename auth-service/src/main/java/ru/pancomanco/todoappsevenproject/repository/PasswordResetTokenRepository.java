package ru.pancomanco.todoappsevenproject.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.pancomanco.todoappsevenproject.entity.PasswordResetToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from PasswordResetToken t
            join fetch t.user
            where t.tokenHash = :tokenHash
            """)
    Optional<PasswordResetToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from PasswordResetToken t
            join fetch t.user
            where t.user.id = :userId
              and t.usedAt is null
            order by t.createdAt desc
            """)
    List<PasswordResetToken> findActiveTokensForUpdate(
            @Param("userId") Long userId,
            Pageable pageable
    );

    default Optional<PasswordResetToken> findLatestActiveTokenForUpdate(Long userId) {
        return findActiveTokensForUpdate(userId, PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }

    @Modifying
    @Query("""
            update PasswordResetToken t
            set t.usedAt = :now
            where t.user.id = :userId
              and t.usedAt is null
            """)
    int markAllActiveTokensAsUsedByUserId(
            @Param("userId") Long userId,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
            delete from PasswordResetToken t
            where t.expiresAt < :now
               or (
                    t.usedAt is not null
                    and t.usedAt < :usedBefore
               )
            """)
    int deleteExpiredOrUsedBefore(
            @Param("now") Instant now,
            @Param("usedBefore") Instant usedBefore
    );
}
