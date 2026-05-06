package com.example.fleet.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DriverController using @SpringBootTest, MockMvc, and Testcontainers PostgreSQL.
 * Tests the full HTTP request/response cycle for POST /api/v1/drivers including authentication,
 * validation, service orchestration, and LGPD compliance (CNH must not appear in error responses).
 *
 * Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 10.4
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class DriverControllerIntegrationTest {

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

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a unique user via POST /api/v1/users and returns the userId, email, and password.
     * Returns String[] { userId, email, password }.
     */
    private String[] seedUser() throws Exception {
        String email = "driver-test-" + UUID.randomUUID() + "@example.com";
        String password = "S3cur3P@ss";

        Map<String, String> createRequest = Map.of(
                "name", "Test Driver User",
                "email", email,
                "password", password,
                "phoneNumber", "+1-555-0200"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String userId = objectMapper.readTree(responseBody).get("id").asText();

        return new String[]{userId, email, password};
    }

    /**
     * Logs in with the given credentials and returns the Bearer JWT token.
     */
    private String obtainJwt(String email, String password) throws Exception {
        Map<String, String> loginRequest = Map.of("email", email, "password", password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("token").asText();
    }

    /** Returns a valid CreateDriverRequest body map for the given userId with a unique CNH. */
    private Map<String, Object> validDriverRequest(String userId) {
        return Map.of(
                "userId", userId,
                "cnh", "CNH" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(),
                "status", "ACTIVE"
        );
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // -----------------------------------------------------------------------
    // Unauthenticated access (HTTP 401)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/drivers without JWT")
    class UnauthenticatedAccess {

        @Test
        @DisplayName("returns HTTP 401 when no Authorization header is provided")
        void createDriver_noJwt_returns401() throws Exception {
            String[] user = seedUser();
            Map<String, Object> request = validDriverRequest(user[0]);

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns HTTP 401 when Authorization header contains an invalid token")
        void createDriver_invalidJwt_returns401() throws Exception {
            String[] user = seedUser();
            Map<String, Object> request = validDriverRequest(user[0]);

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer this.is.not.a.valid.token")
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -----------------------------------------------------------------------
    // Successful creation (HTTP 201)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/drivers with valid JWT and valid body")
    class ValidRequest {

        private String userId;
        private String jwt;

        @BeforeEach
        void setup() throws Exception {
            String[] user = seedUser();
            userId = user[0];
            jwt = obtainJwt(user[1], user[2]);
        }

        @Test
        @DisplayName("returns HTTP 201 Created")
        void createDriver_validRequest_returns201() throws Exception {
            Map<String, Object> request = validDriverRequest(userId);

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("response contains id, userId, cnh, and status fields")
        void createDriver_validRequest_responseContainsAllFields() throws Exception {
            Map<String, Object> request = validDriverRequest(userId);

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.userId").value(userId))
                    .andExpect(jsonPath("$.cnh").value(request.get("cnh")))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("id field is a valid UUID format")
        void createDriver_validRequest_idIsUUID() throws Exception {
            Map<String, Object> request = validDriverRequest(userId);

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(matchesPattern(
                            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
                    )));
        }

        @Test
        @DisplayName("response Content-Type is application/json")
        void createDriver_validRequest_responseIsJson() throws Exception {
            Map<String, Object> request = validDriverRequest(userId);

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("INACTIVE status is persisted and returned correctly")
        void createDriver_inactiveStatus_returnsInactive() throws Exception {
            Map<String, Object> request = Map.of(
                    "userId", userId,
                    "cnh", "INACTIVE" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase(),
                    "status", "INACTIVE"
            );

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("INACTIVE"));
        }
    }

    // -----------------------------------------------------------------------
    // Validation errors (HTTP 400)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/drivers with missing or invalid fields")
    class ValidationErrors {

        private String userId;
        private String jwt;

        @BeforeEach
        void setup() throws Exception {
            String[] user = seedUser();
            userId = user[0];
            jwt = obtainJwt(user[1], user[2]);
        }

        @Test
        @DisplayName("missing cnh returns HTTP 400")
        void createDriver_missingCnh_returns400() throws Exception {
            Map<String, Object> request = Map.of(
                    "userId", userId,
                    "status", "ACTIVE"
            );

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("blank cnh returns HTTP 400 with descriptive error")
        void createDriver_blankCnh_returns400() throws Exception {
            Map<String, Object> request = Map.of(
                    "userId", userId,
                    "cnh", "   ",
                    "status", "ACTIVE"
            );

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("missing userId returns HTTP 400 with descriptive error")
        void createDriver_missingUserId_returns400() throws Exception {
            Map<String, Object> request = Map.of(
                    "cnh", "ABC12345",
                    "status", "ACTIVE"
            );

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("missing status returns HTTP 400 with descriptive error")
        void createDriver_missingStatus_returns400() throws Exception {
            Map<String, Object> request = Map.of(
                    "userId", userId,
                    "cnh", "ABC12345"
            );

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("cnh exceeding 20 characters returns HTTP 400")
        void createDriver_cnhTooLong_returns400() throws Exception {
            Map<String, Object> request = Map.of(
                    "userId", userId,
                    "cnh", "ABCDEFGHIJ12345678901", // 21 characters
                    "status", "ACTIVE"
            );

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("non-alphanumeric cnh returns HTTP 400")
        void createDriver_nonAlphanumericCnh_returns400() throws Exception {
            Map<String, Object> request = Map.of(
                    "userId", userId,
                    "cnh", "ABC-123!",
                    "status", "ACTIVE"
            );

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    // -----------------------------------------------------------------------
    // Non-existent userId (HTTP 404)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/drivers with non-existent userId")
    class UserNotFound {

        private String jwt;

        @BeforeEach
        void setup() throws Exception {
            String[] user = seedUser();
            jwt = obtainJwt(user[1], user[2]);
        }

        @Test
        @DisplayName("non-existent userId returns HTTP 404")
        void createDriver_nonExistentUserId_returns404() throws Exception {
            Map<String, Object> request = Map.of(
                    "userId", UUID.randomUUID().toString(), // guaranteed not to exist
                    "cnh", "NOTFOUND01",
                    "status", "ACTIVE"
            );

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("404 error response contains descriptive message")
        void createDriver_nonExistentUserId_responseContainsError() throws Exception {
            String nonExistentUserId = UUID.randomUUID().toString();
            Map<String, Object> request = Map.of(
                    "userId", nonExistentUserId,
                    "cnh", "NOTFOUND02",
                    "status", "ACTIVE"
            );

            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value(containsString("not found")));
        }
    }

    // -----------------------------------------------------------------------
    // Duplicate CNH (HTTP 409) — LGPD compliance
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/drivers with duplicate CNH")
    class DuplicateCnh {

        private String userId;
        private String jwt;
        private Map<String, Object> firstRequest;

        @BeforeEach
        void setup() throws Exception {
            String[] user = seedUser();
            userId = user[0];
            jwt = obtainJwt(user[1], user[2]);

            // Register the first driver to establish the duplicate CNH
            firstRequest = validDriverRequest(userId);
            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(firstRequest)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("duplicate CNH returns HTTP 409 Conflict")
        void createDriver_duplicateCnh_returns409() throws Exception {
            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(firstRequest)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("409 response contains a generic error message")
        void createDriver_duplicateCnh_responseContainsGenericError() throws Exception {
            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(firstRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.error").isNotEmpty());
        }

        /**
         * LGPD compliance: the raw CNH value must NOT appear in the 409 error response body.
         * Requirement 10.4 — GlobalExceptionHandler SHALL NOT include the raw CNH value in error responses.
         */
        @Test
        @DisplayName("409 response does NOT contain the raw CNH value (LGPD compliance)")
        void createDriver_duplicateCnh_responseDoesNotContainCnhValue() throws Exception {
            String cnh = (String) firstRequest.get("cnh");

            String responseBody = mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(firstRequest)))
                    .andExpect(status().isConflict())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // The raw CNH value must not appear anywhere in the response body
            org.assertj.core.api.Assertions.assertThat(responseBody)
                    .as("LGPD compliance: raw CNH value must not appear in 409 error response")
                    .doesNotContain(cnh);
        }

        @Test
        @DisplayName("409 error message is generic and does not expose PII")
        void createDriver_duplicateCnh_errorMessageIsGeneric() throws Exception {
            mockMvc.perform(post("/api/v1/drivers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(firstRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("CNH already registered"));
        }
    }
}
