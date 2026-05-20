package ru.pancomanco.todoappsevenproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.pancomanco.todoappsevenproject.entity.AuthProviderEnum;
import ru.pancomanco.todoappsevenproject.entity.LinkedAccount;

import java.util.Optional;

@Repository
public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {
    @Query("""
            select la
            from LinkedAccount la
            join fetch la.user
            where la.provider = :provider
              and la.providerUserId = :providerUserId
            """)
    Optional<LinkedAccount> findByProviderAndProviderUserIdWithUser(
            @Param("provider") AuthProviderEnum provider,
            @Param("providerUserId") String providerUserId
    );
}
