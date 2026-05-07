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

import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for VehicleController using @SpringBootTest, MockMvc, and Testcontainers PostgreSQL.
 * Tests the full HTTP request/response cycle for POST /api/v1/vehicles including authentication,
 * validation, service orchestration, and error handling.
 *
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class VehicleControllerIntegrationTest {

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
        String email = "vehicle-test-" + UUID.randomUUID() + "@example.com";
        String password = "S3cur3P@ss";

        Map<String, String> createRequest = Map.of(
                "name", "Test Vehicle User",
                "email", email,
                "password", password,
                "phoneNumber", "+1-555-0300"
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

    /**
     * Creates a driver via POST /api/v1/drivers using the given JWT and returns the driver UUID as a String.
     */
    private String seedDriver(String jwt) throws Exception {
        String[] user = seedUser();
        String userId = user[0];

        Map<String, Object> driverRequest = Map.of(
                "userId", userId,
                "cnh", "CNH" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(),
                "status", "ACTIVE"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                        .content(toJson(driverRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("id").asText();
    }

    /** Returns a valid CreateVehicleRequest body map with a unique plate. */
    private Map<String, Object> validVehicleRequest() {
        return new HashMap<>(Map.of(
                "plate", "PLT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "brand", "Toyota",
                "model", "Corolla",
                "year", Year.now().getValue()
        ));
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // -----------------------------------------------------------------------
    // Successful creation without driverId (HTTP 201)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/vehicles with valid body and no driverId")
    class ValidRequestWithoutDriverId {

        private String jwt;

        @BeforeEach
        void setup() throws Exception {
            String[] user = seedUser();
            jwt = obtainJwt(user[1], user[2]);
        }

        @Test
        @DisplayName("returns HTTP 201 with all response fields and null driverId")
        void createVehicle_validBodyWithoutDriverId_returns201() throws Exception {
            Map<String, Object> request = validVehicleRequest();

            mockMvc.perform(post("/api/v1/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.id").value(matchesPattern(
                            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
                    )))
                    .andExpect(jsonPath("$.plate").value(request.get("plate")))
                    .andExpect(jsonPath("$.brand").value(request.get("brand")))
                    .andExpect(jsonPath("$.model").value(request.get("model")))
                    .andExpect(jsonPath("$.year").value(request.get("year")))
                    .andExpect(jsonPath("$.driverId").doesNotExist());
        }
    }

    // -----------------------------------------------------------------------
    // Successful creation with existing driverId (HTTP 201)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/vehicles with valid body and existing driverId")
    class ValidRequestWithDriverId {

        private String jwt;
        private String driverId;

        @BeforeEach
        void setup() throws Exception {
            String[] user = seedUser();
            jwt = obtainJwt(user[1], user[2]);
            driverId = seedDriver(jwt);
        }

        @Test
        @DisplayName("returns HTTP 201 with driverId matching the seeded driver")
        void createVehicle_validBodyWithDriverId_returns201WithDriverId() throws Exception {
            Map<String, Object> request = validVehicleRequest();
            request.put("driverId", driverId);

            mockMvc.perform(post("/api/v1/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.driverId").value(driverId));
        }
    }

    // -----------------------------------------------------------------------
    // Validation errors (HTTP 400)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/vehicles with missing or invalid fields")
    class ValidationErrors {

        private String jwt;

        @BeforeEach
        void setup() throws Exception {
            String[] user = seedUser();
            jwt = obtainJwt(user[1], user[2]);
        }

        @Test
        @DisplayName("missing plate returns HTTP 400 with error field")
        void createVehicle_missingPlate_returns400() throws Exception {
            Map<String, Object> request = validVehicleRequest();
            request.remove("plate");

            mockMvc.perform(post("/api/v1/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("year = 1885 returns HTTP 400 with error field")
        void createVehicle_year1885_returns400() throws Exception {
            Map<String, Object> request = validVehicleRequest();
            request.put("year", 1885);

            mockMvc.perform(post("/api/v1/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("year = currentYear + 2 returns HTTP 400 with error field")
        void createVehicle_yearCurrentPlusTwo_returns400() throws Exception {
            int futureYear = Year.now().getValue() + 2;
            Map<String, Object> request = validVehicleRequest();
            request.put("year", futureYear);

            mockMvc.perform(post("/api/v1/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    // -----------------------------------------------------------------------
    // Duplicate plate (HTTP 409)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/vehicles with duplicate plate")
    class DuplicatePlate {

        private String jwt;
        private Map<String, Object> firstRequest;

        @BeforeEach
        void setup() throws Exception {
            String[] user = seedUser();
            jwt = obtainJwt(user[1], user[2]);

            // Register the first vehicle to establish the duplicate plate
            firstRequest = validVehicleRequest();
            mockMvc.perform(post("/api/v1/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(firstRequest)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("duplicate plate returns HTTP 409 with 'Plate already registered' error")
        void createVehicle_duplicatePlate_returns409() throws Exception {
            mockMvc.perform(post("/api/v1/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(firstRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Plate already registered"));
        }
    }

    // -----------------------------------------------------------------------
    // Non-existent driverId (HTTP 404)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/vehicles with non-existent driverId")
    class DriverNotFound {

        private String jwt;

        @BeforeEach
        void setup() throws Exception {
            String[] user = seedUser();
            jwt = obtainJwt(user[1], user[2]);
        }

        @Test
        @DisplayName("non-existent driverId returns HTTP 404 with error field")
        void createVehicle_nonExistentDriverId_returns404() throws Exception {
            Map<String, Object> request = validVehicleRequest();
            request.put("driverId", UUID.randomUUID().toString());

            mockMvc.perform(post("/api/v1/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + jwt)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    // -----------------------------------------------------------------------
    // Unauthenticated access (HTTP 401)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/vehicles without JWT")
    class UnauthenticatedAccess {

        @Test
        @DisplayName("returns HTTP 401 when no Authorization header is provided")
        void createVehicle_noJwt_returns401() throws Exception {
            Map<String, Object> request = validVehicleRequest();

            mockMvc.perform(post("/api/v1/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
