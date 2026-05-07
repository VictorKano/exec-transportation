package com.example.fleet.infrastructure;

import com.example.fleet.application.service.UserService;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for UserController using @SpringBootTest, MockMvc, and Testcontainers PostgreSQL.
 * Tests the full HTTP request/response cycle including validation, service layer, and database persistence.
 *
 * Requirements: 8.3
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Container
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
    private UserService userService;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Returns a valid request with a unique email to avoid duplicate-key conflicts. */
    private Map<String, String> uniqueValidUserRequest() {
        String unique = "user-" + UUID.randomUUID() + "@example.com";
        return Map.of(
                "name", "Jane Doe",
                "email", unique,
                "password", "S3cur3P@ss",
                "phoneNumber", "+1-555-0100"
        );
    }

    private String toJson(Map<String, String> map) throws Exception {
        return objectMapper.writeValueAsString(map);
    }

    // -----------------------------------------------------------------------
    // Successful creation (HTTP 201)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/users with valid body")
    class ValidRequest {

        @Test
        @DisplayName("returns HTTP 201 Created")
        void createUser_validBody_returns201() throws Exception {
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(uniqueValidUserRequest())))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("response contains id, name, email, phoneNumber fields")
        void createUser_validBody_responseContainsAllFields() throws Exception {
            Map<String, String> request = uniqueValidUserRequest();
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").value("Jane Doe"))
                    .andExpect(jsonPath("$.email").value(request.get("email")))
                    .andExpect(jsonPath("$.phoneNumber").value("+1-555-0100"));
        }

        @Test
        @DisplayName("response does not contain password field")
        void createUser_validBody_responseDoesNotContainPassword() throws Exception {
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(uniqueValidUserRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.hashedPassword").doesNotExist());
        }

        @Test
        @DisplayName("id field is a valid UUID format")
        void createUser_validBody_idIsUUID() throws Exception {
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(uniqueValidUserRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(matchesPattern(
                            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
                    )));
        }

        @Test
        @DisplayName("response Content-Type is application/json")
        void createUser_validBody_responseIsJson() throws Exception {
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(uniqueValidUserRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    // -----------------------------------------------------------------------
    // Validation errors (HTTP 400)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/users with missing required fields")
    class MissingRequiredFields {

        @Test
        @DisplayName("missing name returns HTTP 400 with descriptive error")
        void createUser_missingName_returns400() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "jane.doe@example.com",
                    "password", "S3cur3P@ss",
                    "phoneNumber", "+1-555-0100"
            );

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("name")));
        }

        @Test
        @DisplayName("missing email returns HTTP 400 with descriptive error")
        void createUser_missingEmail_returns400() throws Exception {
            Map<String, String> request = Map.of(
                    "name", "Jane Doe",
                    "password", "S3cur3P@ss",
                    "phoneNumber", "+1-555-0100"
            );

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("email")));
        }

        @Test
        @DisplayName("missing password returns HTTP 400 with descriptive error")
        void createUser_missingPassword_returns400() throws Exception {
            Map<String, String> request = Map.of(
                    "name", "Jane Doe",
                    "email", "jane.doe@example.com",
                    "phoneNumber", "+1-555-0100"
            );

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("password")));
        }

        @Test
        @DisplayName("missing phoneNumber returns HTTP 400 with descriptive error")
        void createUser_missingPhoneNumber_returns400() throws Exception {
            Map<String, String> request = Map.of(
                    "name", "Jane Doe",
                    "email", "jane.doe@example.com",
                    "password", "S3cur3P@ss"
            );

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("phoneNumber")));
        }

        @Test
        @DisplayName("blank name returns HTTP 400 with descriptive error")
        void createUser_blankName_returns400() throws Exception {
            Map<String, String> request = Map.of(
                    "name", "   ",
                    "email", "jane.doe@example.com",
                    "password", "S3cur3P@ss",
                    "phoneNumber", "+1-555-0100"
            );

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("name")));
        }

        @Test
        @DisplayName("invalid email format returns HTTP 400 with descriptive error")
        void createUser_invalidEmail_returns400() throws Exception {
            Map<String, String> request = Map.of(
                    "name", "Jane Doe",
                    "email", "not-an-email",
                    "password", "S3cur3P@ss",
                    "phoneNumber", "+1-555-0100"
            );

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("email")));
        }

        @Test
        @DisplayName("password shorter than 8 characters returns HTTP 400 with descriptive error")
        void createUser_shortPassword_returns400() throws Exception {
            Map<String, String> request = Map.of(
                    "name", "Jane Doe",
                    "email", "jane.doe@example.com",
                    "password", "short",
                    "phoneNumber", "+1-555-0100"
            );

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("password")));
        }
    }

    // -----------------------------------------------------------------------
    // Duplicate email (HTTP 409)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/users with duplicate email")
    class DuplicateEmail {

        private Map<String, String> duplicateRequest;

        @BeforeEach
        void createFirstUser() throws Exception {
            // Use a unique email per test run to avoid cross-test contamination
            duplicateRequest = uniqueValidUserRequest();
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(duplicateRequest)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("duplicate email returns HTTP 409 Conflict")
        void createUser_duplicateEmail_returns409() throws Exception {
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(duplicateRequest)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("duplicate email response contains descriptive error message")
        void createUser_duplicateEmail_responseContainsError() throws Exception {
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(duplicateRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.error").value(containsString("email")))
                    .andExpect(jsonPath("$.error").value(containsString(duplicateRequest.get("email"))));
        }
    }

    // -----------------------------------------------------------------------
    // Unexpected exception (HTTP 500)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/users with unexpected exception")
    class UnexpectedException {

        @AfterEach
        void resetSpy() {
            reset(userService);
        }

        @Test
        @DisplayName("service throwing RuntimeException returns HTTP 500 with generic message")
        void createUser_unexpectedException_returns500() throws Exception {
            // Mock the service to throw an unexpected exception
            doThrow(new RuntimeException("Database connection failed"))
                    .when(userService).createUser(any());

            Map<String, String> request = Map.of(
                    "name", "John Smith",
                    "email", "john.smith@example.com",
                    "password", "P@ssw0rd",
                    "phoneNumber", "+1-555-9999"
            );

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("An unexpected error occurred"));
        }

        @Test
        @DisplayName("HTTP 500 response does not expose internal exception details")
        void createUser_unexpectedException_doesNotExposeInternalDetails() throws Exception {
            doThrow(new RuntimeException("Database connection failed"))
                    .when(userService).createUser(any());

            Map<String, String> request = Map.of(
                    "name", "John Smith",
                    "email", "john.smith@example.com",
                    "password", "P@ssw0rd",
                    "phoneNumber", "+1-555-9999"
            );

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value(not(containsString("Database"))))
                    .andExpect(jsonPath("$.error").value(not(containsString("RuntimeException"))))
                    .andExpect(jsonPath("$.error").value(not(containsString("stack"))));
        }
    }
}
