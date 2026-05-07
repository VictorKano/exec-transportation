package com.example.fleet.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenApiConfig}.
 *
 * These tests instantiate the config class directly — no Spring context is
 * needed — so they run fast and in isolation.
 */
class OpenApiConfigTest {

    private OpenAPI openAPI;

    @BeforeEach
    void setUp() {
        openAPI = new OpenApiConfig().fleetOpenAPI();
    }

    // -------------------------------------------------------------------------
    // Requirement 2.1 — title
    // -------------------------------------------------------------------------

    @Test
    void openAPI_bean_has_expected_title() {
        assertThat(openAPI.getInfo().getTitle())
                .isEqualTo("Fleet Management API");
    }

    // -------------------------------------------------------------------------
    // Requirement 2.2 — version
    // -------------------------------------------------------------------------

    @Test
    void openAPI_bean_has_expected_version() {
        assertThat(openAPI.getInfo().getVersion())
                .isEqualTo("1.0.0");
    }

    // -------------------------------------------------------------------------
    // Requirement 2.3 — description
    // -------------------------------------------------------------------------

    @Test
    void openAPI_bean_has_non_blank_description() {
        assertThat(openAPI.getInfo().getDescription())
                .isNotBlank();
    }

    // -------------------------------------------------------------------------
    // Requirement 3.1 — bearerAuth SecurityScheme
    // -------------------------------------------------------------------------

    @Test
    void bearerAuth_security_scheme_is_registered() {
        assertThat(openAPI.getComponents().getSecuritySchemes())
                .containsKey("bearerAuth");
    }

    @Test
    void bearerAuth_security_scheme_has_type_http() {
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
    }

    @Test
    void bearerAuth_security_scheme_has_scheme_bearer() {
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(scheme.getScheme()).isEqualTo("bearer");
    }

    @Test
    void bearerAuth_security_scheme_has_bearer_format_jwt() {
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }

    // -------------------------------------------------------------------------
    // Requirement 3.2 — global SecurityRequirement
    // -------------------------------------------------------------------------

    @Test
    void global_security_requirement_contains_bearerAuth() {
        assertThat(openAPI.getSecurity())
                .isNotNull()
                .isNotEmpty();

        boolean hasBearerAuth = openAPI.getSecurity().stream()
                .map(SecurityRequirement::keySet)
                .anyMatch(keys -> keys.contains("bearerAuth"));

        assertThat(hasBearerAuth)
                .as("At least one global SecurityRequirement should reference 'bearerAuth'")
                .isTrue();
    }
}
