package ru.pancomanco.taskservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.pancomanco.taskservice.dto.request.CreateTaskRequestDto;
import ru.pancomanco.taskservice.dto.request.UpdateTaskRequestDto;
import ru.pancomanco.taskservice.dto.response.TaskResponseDto;
import ru.pancomanco.taskservice.service.TaskService;
import ru.pancomanco.taskservice.service.TaskUserService;

import java.awt.print.Pageable;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskUserService taskUserService;

    private Long ownerId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

//    @GetMapping
//    public List<TaskResponseDto> getTasks(@AuthenticationPrincipal Jwt jwt) {
//        return taskService.getTasks(ownerId(jwt));
//    }

    @GetMapping
    public ResponseEntity<Page<TaskResponseDto>> getTasks(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, page = 0, sort = "createdAt") Pageable pageable
    ) {

        Page<TaskResponseDto> tasks = taskService.getTasks(ownerId(jwt), pageable);

        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public TaskResponseDto getTask(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return taskService.getTask(id, ownerId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponseDto createTask(
            @Valid @RequestBody CreateTaskRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = ownerId(jwt);
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        taskUserService.ensureUserExists(ownerId, email, name);
        return taskService.createTask(request, ownerId);
    }

    @PutMapping("/{id}")
    public TaskResponseDto updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        return taskService.updateTask(id, request, ownerId(jwt));
    }

    @PatchMapping("/{id}/complete")
    public TaskResponseDto complete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return taskService.setCompleted(id, true, ownerId(jwt));
    }

    @PatchMapping("/{id}/incomplete")
    public TaskResponseDto incomplete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return taskService.setCompleted(id, false, ownerId(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        taskService.deleteTask(id, ownerId(jwt));
    }
}
