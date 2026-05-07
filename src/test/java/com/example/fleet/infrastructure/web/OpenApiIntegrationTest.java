package com.example.fleet.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the OpenAPI / Swagger UI integration.
 *
 * Verifies:
 * - Swagger UI and OpenAPI spec paths are accessible without authentication (Requirements 1.2, 1.3, 4.1–4.4)
 * - Protected endpoints still require a JWT (Requirements 3.4, 4.5)
 * - The generated spec contains the expected tags, operations, response codes, and schemas
 *   (Requirements 5.1–5.6, 6.1–6.5, 7.1–7.3)
 *
 * The spec JSON is fetched once per test class and shared across all nested test classes
 * to avoid redundant HTTP calls.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class OpenApiIntegrationTest {

    // -------------------------------------------------------------------------
    // Testcontainers — shared PostgreSQL instance for the whole test class
    // -------------------------------------------------------------------------

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
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    /**
     * Parsed OpenAPI spec JSON, fetched once and reused across all nested tests.
     * Populated by {@link #fetchApiDocs()}.
     */
    static JsonNode apiDocs;

    @BeforeAll
    static void fetchApiDocs(@Autowired MockMvc mockMvc,
                             @Autowired ObjectMapper objectMapper) throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        apiDocs = objectMapper.readTree(result.getResponse().getContentAsString());
    }

    // =========================================================================
    // 9.2 — GET /v3/api-docs returns HTTP 200 without authentication
    // =========================================================================

    @Nested
    @DisplayName("9.2 GET /v3/api-docs is accessible without authentication")
    class ApiDocsAccessibility {

        @Test
        @DisplayName("returns HTTP 200 without an Authorization header")
        void apiDocs_noAuth_returns200() throws Exception {
            // Requirement 1.2, 4.2, 4.4
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // 9.3 — GET /swagger-ui/index.html returns HTTP 200 or 302 without auth
    // =========================================================================

    @Nested
    @DisplayName("9.3 GET /swagger-ui/index.html is accessible without authentication")
    class SwaggerUiAccessibility {

        @Test
        @DisplayName("returns HTTP 200 or 302 without an Authorization header")
        void swaggerUi_noAuth_returns200Or302() throws Exception {
            // Requirement 1.3, 4.1, 4.3
            int status = mockMvc.perform(get("/swagger-ui/index.html"))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            assertThat(status)
                    .as("Swagger UI should be accessible without auth (200 or 302)")
                    .isIn(200, 302);
        }
    }

    // =========================================================================
    // 9.4 — POST /api/v1/drivers without Authorization header returns HTTP 401
    // =========================================================================

    @Nested
    @DisplayName("9.4 POST /api/v1/drivers without Authorization header returns HTTP 401")
    class ProtectedEndpointSecurity {

        @Test
        @DisplayName("returns HTTP 401 when no Authorization header is present")
        void createDriver_noAuth_returns401() throws Exception {
            // Requirement 3.4, 4.5
            mockMvc.perform(post("/api/v1/drivers")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // 9.5 — Spec JSON contains tags "Authentication", "Users", and "Drivers"
    // =========================================================================

    @Nested
    @DisplayName("9.5 OpenAPI spec contains expected tags")
    class SpecTags {

        @Test
        @DisplayName("spec contains the 'Authentication' tag")
        void spec_contains_authentication_tag() {
            // Requirement 5.1
            assertThat(tagNames())
                    .as("Spec should contain the 'Authentication' tag")
                    .contains("Authentication");
        }

        @Test
        @DisplayName("spec contains the 'Users' tag")
        void spec_contains_users_tag() {
            // Requirement 5.2
            assertThat(tagNames())
                    .as("Spec should contain the 'Users' tag")
                    .contains("Users");
        }

        @Test
        @DisplayName("spec contains the 'Drivers' tag")
        void spec_contains_drivers_tag() {
            // Requirement 5.3
            assertThat(tagNames())
                    .as("Spec should contain the 'Drivers' tag")
                    .contains("Drivers");
        }

        /** Collects all tag names from the top-level "tags" array in the spec. */
        private java.util.List<String> tagNames() {
            java.util.List<String> names = new java.util.ArrayList<>();
            JsonNode tags = apiDocs.path("tags");
            if (tags.isArray()) {
                for (JsonNode tag : tags) {
                    names.add(tag.path("name").asText());
                }
            }
            return names;
        }
    }

    // =========================================================================
    // 9.6 — Login operation documents response codes 200 and 401
    // =========================================================================

    @Nested
    @DisplayName("9.6 Login operation documents response codes 200 and 401")
    class LoginOperationResponseCodes {

        @Test
        @DisplayName("POST /api/v1/auth/login documents HTTP 200")
        void login_operation_documents_200() {
            // Requirement 5.4
            JsonNode responses = loginOperationResponses();
            assertThat(responses.has("200"))
                    .as("Login operation should document HTTP 200")
                    .isTrue();
        }

        @Test
        @DisplayName("POST /api/v1/auth/login documents HTTP 401")
        void login_operation_documents_401() {
            // Requirement 5.4
            JsonNode responses = loginOperationResponses();
            assertThat(responses.has("401"))
                    .as("Login operation should document HTTP 401")
                    .isTrue();
        }

        private JsonNode loginOperationResponses() {
            return apiDocs
                    .path("paths")
                    .path("/api/v1/auth/login")
                    .path("post")
                    .path("responses");
        }
    }

    // =========================================================================
    // 9.7 — createUser operation documents response codes 201 and 409
    // =========================================================================

    @Nested
    @DisplayName("9.7 createUser operation documents response codes 201 and 409")
    class CreateUserOperationResponseCodes {

        @Test
        @DisplayName("POST /api/v1/users documents HTTP 201")
        void createUser_operation_documents_201() {
            // Requirement 5.5
            JsonNode responses = createUserOperationResponses();
            assertThat(responses.has("201"))
                    .as("createUser operation should document HTTP 201")
                    .isTrue();
        }

        @Test
        @DisplayName("POST /api/v1/users documents HTTP 409")
        void createUser_operation_documents_409() {
            // Requirement 5.5
            JsonNode responses = createUserOperationResponses();
            assertThat(responses.has("409"))
                    .as("createUser operation should document HTTP 409")
                    .isTrue();
        }

        private JsonNode createUserOperationResponses() {
            return apiDocs
                    .path("paths")
                    .path("/api/v1/users")
                    .path("post")
                    .path("responses");
        }
    }

    // =========================================================================
    // 9.8 — createDriver operation documents response codes 201, 401, and 409
    // =========================================================================

    @Nested
    @DisplayName("9.8 createDriver operation documents response codes 201, 401, and 409")
    class CreateDriverOperationResponseCodes {

        @Test
        @DisplayName("POST /api/v1/drivers documents HTTP 201")
        void createDriver_operation_documents_201() {
            // Requirement 5.6
            JsonNode responses = createDriverOperationResponses();
            assertThat(responses.has("201"))
                    .as("createDriver operation should document HTTP 201")
                    .isTrue();
        }

        @Test
        @DisplayName("POST /api/v1/drivers documents HTTP 401")
        void createDriver_operation_documents_401() {
            // Requirement 5.6
            JsonNode responses = createDriverOperationResponses();
            assertThat(responses.has("401"))
                    .as("createDriver operation should document HTTP 401")
                    .isTrue();
        }

        @Test
        @DisplayName("POST /api/v1/drivers documents HTTP 409")
        void createDriver_operation_documents_409() {
            // Requirement 5.6
            JsonNode responses = createDriverOperationResponses();
            assertThat(responses.has("409"))
                    .as("createDriver operation should document HTTP 409")
                    .isTrue();
        }

        private JsonNode createDriverOperationResponses() {
            return apiDocs
                    .path("paths")
                    .path("/api/v1/drivers")
                    .path("post")
                    .path("responses");
        }
    }

    // =========================================================================
    // 9.9 — Spec schemas contain expected fields for request/response types
    // =========================================================================

    @Nested
    @DisplayName("9.9 Spec schemas contain expected fields")
    class SpecSchemas {

        @Test
        @DisplayName("LoginRequest schema has 'email' and 'password' fields")
        void loginRequest_schema_has_expected_fields() {
            // Requirement 6.1
            JsonNode properties = schemaProperties("LoginRequest");
            assertThat(properties.has("email"))
                    .as("LoginRequest schema should have 'email' field")
                    .isTrue();
            assertThat(properties.has("password"))
                    .as("LoginRequest schema should have 'password' field")
                    .isTrue();
        }

        @Test
        @DisplayName("CreateUserRequest schema has 'name', 'email', 'password', and 'phoneNumber' fields")
        void createUserRequest_schema_has_expected_fields() {
            // Requirement 6.2
            JsonNode properties = schemaProperties("CreateUserRequest");
            assertThat(properties.has("name"))
                    .as("CreateUserRequest schema should have 'name' field")
                    .isTrue();
            assertThat(properties.has("email"))
                    .as("CreateUserRequest schema should have 'email' field")
                    .isTrue();
            assertThat(properties.has("password"))
                    .as("CreateUserRequest schema should have 'password' field")
                    .isTrue();
            assertThat(properties.has("phoneNumber"))
                    .as("CreateUserRequest schema should have 'phoneNumber' field")
                    .isTrue();
        }

        @Test
        @DisplayName("CreateDriverRequest schema has 'userId', 'cnh', and 'status' fields")
        void createDriverRequest_schema_has_expected_fields() {
            // Requirement 6.3
            JsonNode properties = schemaProperties("CreateDriverRequest");
            assertThat(properties.has("userId"))
                    .as("CreateDriverRequest schema should have 'userId' field")
                    .isTrue();
            assertThat(properties.has("cnh"))
                    .as("CreateDriverRequest schema should have 'cnh' field")
                    .isTrue();
            assertThat(properties.has("status"))
                    .as("CreateDriverRequest schema should have 'status' field")
                    .isTrue();
        }

        @Test
        @DisplayName("AuthResponse schema has 'token', 'userId', and 'email' fields")
        void authResponse_schema_has_expected_fields() {
            // Requirement 6.4
            JsonNode properties = schemaProperties("AuthResponse");
            assertThat(properties.has("token"))
                    .as("AuthResponse schema should have 'token' field")
                    .isTrue();
            assertThat(properties.has("userId"))
                    .as("AuthResponse schema should have 'userId' field")
                    .isTrue();
            assertThat(properties.has("email"))
                    .as("AuthResponse schema should have 'email' field")
                    .isTrue();
        }

        @Test
        @DisplayName("DriverResponse schema has 'id', 'userId', 'cnh', and 'status' fields")
        void driverResponse_schema_has_expected_fields() {
            // Requirement 6.5
            JsonNode properties = schemaProperties("DriverResponse");
            assertThat(properties.has("id"))
                    .as("DriverResponse schema should have 'id' field")
                    .isTrue();
            assertThat(properties.has("userId"))
                    .as("DriverResponse schema should have 'userId' field")
                    .isTrue();
            assertThat(properties.has("cnh"))
                    .as("DriverResponse schema should have 'cnh' field")
                    .isTrue();
            assertThat(properties.has("status"))
                    .as("DriverResponse schema should have 'status' field")
                    .isTrue();
        }

        /**
         * Navigates to {@code components.schemas.<schemaName>.properties} in the spec JSON.
         * Returns a missing node (never null) if the path does not exist.
         */
        private JsonNode schemaProperties(String schemaName) {
            return apiDocs
                    .path("components")
                    .path("schemas")
                    .path(schemaName)
                    .path("properties");
        }
    }

    // =========================================================================
    // 9.10 — Error response codes (400, 401, 409) are documented on relevant ops
    // =========================================================================

    @Nested
    @DisplayName("9.10 Error response codes are documented on relevant operations")
    class ErrorResponseDocumentation {

        @Test
        @DisplayName("POST /api/v1/auth/login documents HTTP 400 (validation error)")
        void login_documents_400() {
            // Requirement 7.1, 7.2
            JsonNode responses = apiDocs
                    .path("paths")
                    .path("/api/v1/auth/login")
                    .path("post")
                    .path("responses");
            assertThat(responses.has("400"))
                    .as("Login operation should document HTTP 400")
                    .isTrue();
        }

        @Test
        @DisplayName("POST /api/v1/users documents HTTP 400 (validation error)")
        void createUser_documents_400() {
            // Requirement 7.1, 7.2
            JsonNode responses = apiDocs
                    .path("paths")
                    .path("/api/v1/users")
                    .path("post")
                    .path("responses");
            assertThat(responses.has("400"))
                    .as("createUser operation should document HTTP 400")
                    .isTrue();
        }

        @Test
        @DisplayName("POST /api/v1/drivers documents HTTP 400 (validation error)")
        void createDriver_documents_400() {
            // Requirement 7.1, 7.2
            JsonNode responses = apiDocs
                    .path("paths")
                    .path("/api/v1/drivers")
                    .path("post")
                    .path("responses");
            assertThat(responses.has("400"))
                    .as("createDriver operation should document HTTP 400")
                    .isTrue();
        }

        @Test
        @DisplayName("POST /api/v1/auth/login documents HTTP 401 (invalid credentials)")
        void login_documents_401() {
            // Requirement 7.3
            JsonNode responses = apiDocs
                    .path("paths")
                    .path("/api/v1/auth/login")
                    .path("post")
                    .path("responses");
            assertThat(responses.has("401"))
                    .as("Login operation should document HTTP 401")
                    .isTrue();
        }

        @Test
        @DisplayName("POST /api/v1/users documents HTTP 409 (duplicate email)")
        void createUser_documents_409() {
            // Requirement 7.1, 7.3
            JsonNode responses = apiDocs
                    .path("paths")
                    .path("/api/v1/users")
                    .path("post")
                    .path("responses");
            assertThat(responses.has("409"))
                    .as("createUser operation should document HTTP 409")
                    .isTrue();
        }

        @Test
        @DisplayName("POST /api/v1/drivers documents HTTP 409 (duplicate CNH)")
        void createDriver_documents_409() {
            // Requirement 7.1, 7.3
            JsonNode responses = apiDocs
                    .path("paths")
                    .path("/api/v1/drivers")
                    .path("post")
                    .path("responses");
            assertThat(responses.has("409"))
                    .as("createDriver operation should document HTTP 409")
                    .isTrue();
        }
    }
}
