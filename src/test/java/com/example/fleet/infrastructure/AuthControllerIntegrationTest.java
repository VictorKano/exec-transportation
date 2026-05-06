package com.example.fleet.infrastructure;

import com.example.fleet.application.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController using @SpringBootTest, MockMvc, and Testcontainers PostgreSQL.
 * Tests the full HTTP request/response cycle for POST /api/v1/auth/login including validation,
 * authentication logic, and error handling.
 *
 * Requirements: 9.4
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("fleet_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private AuthenticationService authenticationService;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Creates a unique user via POST /api/v1/users and returns the email and password used. */
    private String[] seedUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String password = "S3cur3P@ss";

        Map<String, String> createRequest = Map.of(
                "name", "Test User",
                "email", email,
                "password", password,
                "phoneNumber", "+1-555-0100"
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createRequest)))
                .andExpect(status().isCreated());

        return new String[]{email, password};
    }

    private String toJson(Map<String, String> map) throws Exception {
        return objectMapper.writeValueAsString(map);
    }

    // -----------------------------------------------------------------------
    // Valid credentials (HTTP 200)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/auth/login with valid credentials")
    class ValidCredentials {

        private String email;
        private String password;

        @BeforeEach
        void createUser() throws Exception {
            String[] credentials = seedUser();
            email = credentials[0];
            password = credentials[1];
        }

        @Test
        @DisplayName("returns HTTP 200 OK")
        void login_validCredentials_returns200() throws Exception {
            Map<String, String> request = Map.of("email", email, "password", password);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("response contains non-blank token")
        void login_validCredentials_responseContainsNonBlankToken() throws Exception {
            Map<String, String> request = Map.of("email", email, "password", password);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.token").value(not(blankOrNullString())));
        }

        @Test
        @DisplayName("response contains userId in valid UUID format")
        void login_validCredentials_responseContainsUuidUserId() throws Exception {
            Map<String, String> request = Map.of("email", email, "password", password);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.userId").value(matchesPattern(
                            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
                    )));
        }

        @Test
        @DisplayName("response contains email matching the registered user")
        void login_validCredentials_responseContainsMatchingEmail() throws Exception {
            Map<String, String> request = Map.of("email", email, "password", password);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(email));
        }
    }

    // -----------------------------------------------------------------------
    // Validation errors (HTTP 400)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/auth/login with blank fields")
    class BlankFields {

        @Test
        @DisplayName("blank email returns HTTP 400 with error field")
        void login_blankEmail_returns400() throws Exception {
            Map<String, String> request = Map.of("email", "   ", "password", "S3cur3P@ss");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("blank password returns HTTP 400 with error field")
        void login_blankPassword_returns400() throws Exception {
            Map<String, String> request = Map.of("email", "user@example.com", "password", "   ");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    // -----------------------------------------------------------------------
    // Invalid credentials (HTTP 401)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/auth/login with invalid credentials")
    class InvalidCredentials {

        private String email;
        private String password;

        @BeforeEach
        void createUser() throws Exception {
            String[] credentials = seedUser();
            email = credentials[0];
            password = credentials[1];
        }

        @Test
        @DisplayName("unknown email returns HTTP 401 with generic error message")
        void login_unknownEmail_returns401() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "unknown-" + UUID.randomUUID() + "@example.com",
                    "password", password
            );

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid email or password"));
        }

        @Test
        @DisplayName("wrong password returns HTTP 401 with generic error message")
        void login_wrongPassword_returns401() throws Exception {
            Map<String, String> request = Map.of("email", email, "password", "WrongP@ss1");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid email or password"));
        }
    }

    // -----------------------------------------------------------------------
    // Unexpected exception (HTTP 500)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/auth/login with unexpected exception")
    class UnexpectedException {

        @AfterEach
        void resetSpy() {
            reset(authenticationService);
        }

        @Test
        @DisplayName("service throwing RuntimeException returns HTTP 500 with generic message")
        void login_unexpectedException_returns500() throws Exception {
            doThrow(new RuntimeException("Unexpected"))
                    .when(authenticationService).login(any());

            Map<String, String> request = Map.of(
                    "email", "user@example.com",
                    "password", "S3cur3P@ss"
            );

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("An unexpected error occurred"));
        }
    }

    // -----------------------------------------------------------------------
    // Publicly accessible (no prior authentication required)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/auth/login endpoint accessibility")
    class PublicAccess {

        private String email;
        private String password;

        @BeforeEach
        void createUser() throws Exception {
            String[] credentials = seedUser();
            email = credentials[0];
            password = credentials[1];
        }

        @Test
        @DisplayName("endpoint is publicly accessible without prior authentication (returns 200, not 401/403)")
        void login_noAuthentication_returns200NotUnauthorized() throws Exception {
            Map<String, String> request = Map.of("email", email, "password", password);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk());
        }
    }
}
