package ru.pancomanco.todoappsevenproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import ru.pancomanco.todoappsevenproject.entity.RefreshToken;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    @Modifying
    @Query("""
            update RefreshToken rt
            set rt.revoked = true
            where rt.user.id = :userId and rt.revoked=false
            """)
    int revokeAllActiveTokensByUserId(@Param("userId") Long userId);
}
