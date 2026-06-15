package ru.pancomanco.todoappsevenproject.controller;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.utility.TestcontainersConfiguration;
import ru.pancomanco.todoappsevenproject.dto.request.LoginRequestDto;
import ru.pancomanco.todoappsevenproject.dto.request.RegisterRequestDto;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.repository.AuthRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.util.Locale;
import java.util.UUID;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
public class AuthorizationControllerIT {
    private static final String VALID_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private MessageSource messageSource;

//    @BeforeEach
//    void setUp() {
//        authRepository.deleteAllInBatch();
//    }

    private String generateUniqueEmail() {
        return "test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private ResultActions performRegister(RegisterRequestDto request) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions performLogin(LoginRequestDto request) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions performLogout() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/logout")
                .header("Accept-Language", "ru"));
    }

    private void expectError(ResultActions result, int status, String message) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(message));
    }

    private String msg(String code) {
        return messageSource.getMessage(code, null, Locale.of("en"));
    }

    @Nested
    class RegistrationTests {

        @Test
        void register_ShouldReturnOk_AndCreateUnverifiedUser() throws Exception {

            String uniqueEmail = generateUniqueEmail();

            performRegister(new RegisterRequestDto("TestUser", uniqueEmail, VALID_PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(uniqueEmail));


            assertThat(authRepository.findByEmail(uniqueEmail))
                    .isPresent()
                    .hasValueSatisfying(user -> {
                        assertThat(user.getEnabled()).isFalse();
                        assertThat(user.getPassword()).isNotEqualTo(VALID_PASSWORD);
                    });
        }

        @Test
        void register_withExistedUsername_ShouldReturnConflictWhenUsernameAlreadyExists() throws Exception {
            String existingEmail = generateUniqueEmail();

            authRepository.save(new User(existingEmail, passwordEncoder.encode(VALID_PASSWORD)));
            expectError(
                    performRegister(new RegisterRequestDto("TestUser",existingEmail, VALID_PASSWORD)),
                    409,
                    msg("error.user.already_exists")
            );
        }

    }

    //todo parametrized test with validation and nested test for login
}
