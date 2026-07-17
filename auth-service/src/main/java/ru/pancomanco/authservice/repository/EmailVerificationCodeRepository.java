package ru.pancomanco.authservice.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.pancomanco.authservice.entity.EmailVerificationCode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationCodeRepository
        extends JpaRepository<EmailVerificationCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from EmailVerificationCode c
            join fetch c.user
            where c.user.id = :userId
              and c.usedAt is null
            order by c.createdAt desc
            """)
    List<EmailVerificationCode> findActiveCodesForUpdate(
            @Param("userId") Long userId,
            Pageable pageable
    );

    default Optional<EmailVerificationCode> findLatestActiveCodeForUpdate(Long userId) {
        return findActiveCodesForUpdate(userId, PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }

    @Modifying
    @Query("""
            update EmailVerificationCode c
            set c.usedAt = :now
            where c.user.id = :userId
              and c.usedAt is null
            """)
    void markAllActiveCodesAsUsedByUserId(
            @Param("userId") Long userId,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
            delete from EmailVerificationCode c
            where c.expiresAt < :now
               or (
                    c.usedAt is not null
                    and c.usedAt < :usedBefore
               )
            """)
    int deleteExpiredOrUsedBefore(
            @Param("now") Instant now,
            @Param("usedBefore") Instant usedBefore
    );

    @Modifying
    @Query("""
            delete from EmailVerificationCode c
            where c.user.id in (
                select u.id
                from User u
                where u.enabled = false
                  and u.createdAt < :userCreatedBefore
            )
            """)
    int deleteCodesForUnverifiedUsersCreatedBefore(
            @Param("userCreatedBefore") Instant userCreatedBefore
    );
}
