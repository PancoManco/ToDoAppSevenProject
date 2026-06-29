package ru.pancomanco.taskservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.taskservice.dto.*;
import ru.pancomanco.taskservice.dto.request.CreateTaskRequestDto;
import ru.pancomanco.taskservice.dto.request.UpdateTaskRequestDto;
import ru.pancomanco.taskservice.dto.response.TaskResponseDto;
import ru.pancomanco.taskservice.entity.Task;
import ru.pancomanco.taskservice.exception.TaskNotFoundException;
import ru.pancomanco.taskservice.repository.TaskRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getTasks(Long ownerId) {
        return taskRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(TaskResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponseDto getTask(Long id, Long ownerId) {
        Task task = taskRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return TaskResponseDto.from(task);
    }

    @Transactional
    public TaskResponseDto createTask(CreateTaskRequestDto request, Long ownerId) {
        Task task = new Task(request.title(), request.description(), ownerId);
        return TaskResponseDto.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDto updateTask(Long id, UpdateTaskRequestDto request, Long ownerId) {
        Task task = taskRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new TaskNotFoundException(id));
        task.setTitle(request.title());
        task.setDescription(request.description());
        return TaskResponseDto.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDto setCompleted(Long id, boolean completed, Long ownerId) {
        Task task = taskRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new TaskNotFoundException(id));
        if (completed) {
            task.markCompleted();
        } else {
            task.markIncomplete();
        }
        return TaskResponseDto.from(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long id, Long ownerId) {
        Task task = taskRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new TaskNotFoundException(id));
        taskRepository.delete(task);
    }
}
