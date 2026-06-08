package ru.pancomanco.todoappsevenproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.pancomanco.todoappsevenproject.entity.User;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Modifying
    @Query("""
            delete from User u
            where u.enabled = false
              and u.createdAt < :cutoff
              and not exists (
                  select 1
                  from LinkedAccount la
                  where la.user = u
              )
            """)
    int deleteUnverifiedUsersCreatedBefore(Instant cutoff);
}
