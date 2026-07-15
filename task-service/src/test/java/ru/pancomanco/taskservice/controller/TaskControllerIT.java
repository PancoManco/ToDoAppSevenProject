package ru.pancomanco.taskservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.pancomanco.taskservice.dto.request.CreateTaskRequestDto;
import ru.pancomanco.taskservice.entity.Task;
import ru.pancomanco.taskservice.repository.TaskRepository;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final Long USER_A = 100L;
    private static final Long USER_B = 200L;

    @BeforeEach
    void clean() {
        taskRepository.deleteAllInBatch();
        taskRepository.flush();

        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    private static RequestPostProcessor asUser(Long userId) {
        return jwt().jwt(builder -> builder
                .subject(String.valueOf(userId))
                .claim("token_type", "access")
                .claim("roles", java.util.List.of("USER")));
    }

    private static RequestPostProcessor asUser(Long userId, String email, String name) {
        return jwt().jwt(builder -> builder
                .subject(String.valueOf(userId))
                .claim("token_type", "access")
                .claim("email", email)
                .claim("name", name)
                .claim("roles", java.util.List.of("USER")));
    }

    private Task createTaskFor(Long ownerId, String title) {
        Task task = new Task(title, "desc", ownerId);
        return taskRepository.save(task);
    }

    @Nested
    class CreateTask {

        @Test
        void createTask_ReturnsCreated() throws Exception {
            CreateTaskRequestDto request = new CreateTaskRequestDto("Поспать наконец то", "несколько дней");

            mockMvc.perform(post("/api/v1/tasks")
                            .with(asUser(USER_A, "custom@test.com", "Custom User"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.title").value("Поспать наконец то"))
                    .andExpect(jsonPath("$.completed").value(false));

            assertThat(taskRepository.findAll())
                    .hasSize(1)
                    .allSatisfy(t -> assertThat(t.getOwnerId()).isEqualTo(USER_A));
        }

        @Test
        void createTask_BlankTitle_ReturnsBadRequest() throws Exception {
            CreateTaskRequestDto request = new CreateTaskRequestDto("", "desc");

            mockMvc.perform(post("/api/v1/tasks")
                            .with(asUser(USER_A))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertThat(taskRepository.findAll()).isEmpty();
        }

        @Test
        void createTask_NoAuth_ReturnsUnauthorized() throws Exception {
            CreateTaskRequestDto request = new CreateTaskRequestDto("Test", "desc");

            mockMvc.perform(post("/api/v1/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class ListTasks {

        @Test
        void getTasks_ReturnsOnlyOwnTasks() throws Exception {
            createTaskFor(USER_A, "Задача A1");
            createTaskFor(USER_A, "Задача A2");
            createTaskFor(USER_B, "Задача B1");

            mockMvc.perform(get("/api/v1/tasks").with(asUser(USER_A)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));

            mockMvc.perform(get("/api/v1/tasks").with(asUser(USER_B)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        void getTasks_Empty() throws Exception {
            mockMvc.perform(get("/api/v1/tasks").with(asUser(USER_A)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    class SingleTask {

        @Test
        void getTask_Own_ReturnsOk() throws Exception {
            Task task = createTaskFor(USER_A, "Моя задача");

            mockMvc.perform(get("/api/v1/tasks/" + task.getId()).with(asUser(USER_A)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Моя задача"));
        }

        @Test
        void updateTask_UpdatesFields() throws Exception {
            Task task = createTaskFor(USER_A, "Старый заголовок");

            mockMvc.perform(put("/api/v1/tasks/" + task.getId())
                            .with(asUser(USER_A))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Новый\",\"description\":\"обновлено\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Новый"));
        }

        @Test
        void complete_MarksCompleted() throws Exception {
            Task task = createTaskFor(USER_A, "Задача");

            mockMvc.perform(patch("/api/v1/tasks/" + task.getId() + "/complete")
                            .with(asUser(USER_A)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(true))
                    .andExpect(jsonPath("$.completedAt").isNotEmpty());
        }

        @Test
        void incomplete_MarksIncomplete() throws Exception {
            Task task = createTaskFor(USER_A, "Задача");
            task.markCompleted();
            taskRepository.save(task);

            mockMvc.perform(patch("/api/v1/tasks/" + task.getId() + "/incomplete")
                            .with(asUser(USER_A)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(false))
                    .andExpect(jsonPath("$.completedAt").doesNotExist());
        }

        @Test
        void deleteTask_Own_ReturnsNoContent() throws Exception {
            Task task = createTaskFor(USER_A, "Удалить");

            mockMvc.perform(delete("/api/v1/tasks/" + task.getId()).with(asUser(USER_A)))
                    .andExpect(status().isNoContent());

            assertThat(taskRepository.findById(task.getId())).isEmpty();
        }
    }

    @Nested
    class OwnerIsolation {

        @Test
        void getTask_OtherUser_ReturnsNotFound() throws Exception {
            Task taskA = createTaskFor(USER_A, "Задача A");

            mockMvc.perform(get("/api/v1/tasks/" + taskA.getId()).with(asUser(USER_B)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updateTask_OtherUser_ReturnsNotFound() throws Exception {
            Task taskA = createTaskFor(USER_A, "Задача A");

            mockMvc.perform(put("/api/v1/tasks/" + taskA.getId())
                            .with(asUser(USER_B))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Взлом\",\"description\":\"x\"}"))
                    .andExpect(status().isNotFound());

            // задача A не изменилась
            Task unchanged = taskRepository.findById(taskA.getId()).orElseThrow();
            assertThat(unchanged.getTitle()).isEqualTo("Задача A");
        }

        @Test
        void deleteTask_OtherUser_ReturnsNotFound() throws Exception {
            Task taskA = createTaskFor(USER_A, "Задача A");

            mockMvc.perform(delete("/api/v1/tasks/" + taskA.getId()).with(asUser(USER_B)))
                    .andExpect(status().isNotFound());

            // задача A всё ещё существует
            assertThat(taskRepository.findById(taskA.getId())).isPresent();
        }
    }
}