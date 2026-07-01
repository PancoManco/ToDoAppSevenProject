package ru.pancomanco.taskservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.pancomanco.taskservice.entity.TaskUser;

public interface TaskUserRepository extends JpaRepository<TaskUser, Long> {

}
