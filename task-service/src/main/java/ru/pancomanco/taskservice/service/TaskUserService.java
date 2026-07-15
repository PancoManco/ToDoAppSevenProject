package ru.pancomanco.taskservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.taskservice.entity.TaskUser;
import ru.pancomanco.taskservice.repository.TaskUserRepository;

@Service
@RequiredArgsConstructor
public class TaskUserService {

    private final TaskUserRepository taskUserRepository;

    @Transactional
    public void ensureUserExists(Long userId, String email, String name) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }

        if (name == null || name.isBlank()) {
            name = email;
        }

        String finalName = name;

        taskUserRepository.findById(userId)
                .ifPresentOrElse(
                        existing -> {
                            existing.setEmail(email);
                            existing.setName(finalName);
                        },
                        () -> taskUserRepository.save(new TaskUser(userId, email, finalName))
                );
    }
}
