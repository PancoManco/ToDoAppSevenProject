package ru.pancomanco.todoappsevenproject.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.pancomanco.todoappsevenproject.entity.RefreshToken;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    //   Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    @Modifying
    @Query("""
        update RefreshToken rt
        set rt.revoked = true
        where rt.tokenHash = :tokenHash
          and rt.revoked = false
        """)
    int revokeByTokenHashIfActive(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select rt
        from RefreshToken rt
        join fetch rt.user
        where rt.tokenHash = :tokenHash
        """)
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            update RefreshToken rt
            set rt.revoked = true
            where rt.user.id = :userId and rt.revoked=false
            """)
    void revokeAllActiveTokensByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("""
            delete from RefreshToken t
            where t.expiresAt < :now
               or (
                    t.revoked = true
                    and t.createdAt < :revokedCreatedBefore
               )
            """)
    int deleteExpiredOrRevokedBefore(
            @Param("now") Instant now,
            @Param("revokedCreatedBefore") Instant revokedCreatedBefore
    );
}
