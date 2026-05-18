package ru.pancomanco.todoappsevenproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pancomanco.todoappsevenproject.entity.AuthProviderEnum;
import ru.pancomanco.todoappsevenproject.entity.LinkedAccount;

import java.util.Optional;

@Repository
public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {
    Optional<LinkedAccount> findByProviderAndProviderUserId(AuthProviderEnum provider, String providerUserId);
}
