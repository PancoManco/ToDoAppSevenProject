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
        if (!taskUserRepository.existsById(userId)) {
            taskUserRepository.save(new TaskUser(userId, email, name));
        }
    }
}
