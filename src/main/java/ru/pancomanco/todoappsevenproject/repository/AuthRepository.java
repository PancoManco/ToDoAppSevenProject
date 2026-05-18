package ru.pancomanco.todoappsevenproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pancomanco.todoappsevenproject.entity.User;

import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<User,Integer> {
    Optional<User> findByUsername(String username);
}
