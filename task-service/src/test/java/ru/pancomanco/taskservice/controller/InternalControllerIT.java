package ru.pancomanco.taskservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.pancomanco.taskservice.config.TestcontainersConfiguration;
import ru.pancomanco.taskservice.entity.Task;
import ru.pancomanco.taskservice.entity.TaskUser;
import ru.pancomanco.taskservice.repository.TaskRepository;
import ru.pancomanco.taskservice.repository.TaskUserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.internal.api-key=test-internal-key"
})
class InternalControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskUserRepository taskUserRepository;

    @BeforeEach
    void clean() {
        taskRepository.deleteAllInBatch();
        taskUserRepository.deleteAllInBatch();
    }

    @Test
    void dailySummary_WithoutApiKey_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/internal/tasks/daily-summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    void dailySummary_WithWrongApiKey_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/internal/tasks/daily-summary")
                        .header("X-Internal-Api-Key", "wrong-key"))
                .andExpect(status().isForbidden());
    }

    @Test
    void dailySummary_WithCorrectApiKey_ReturnsOk() throws Exception {
        taskUserRepository.save(new TaskUser(100L, "user@test.com", "User Test"));

        Task task = new Task("Test task", "desc", 100L);
        taskRepository.save(task);

        mockMvc.perform(get("/internal/tasks/daily-summary")
                        .header("X-Internal-Api-Key", "test-internal-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray());
    }
}
