package com.example.fleet.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration for the Fleet Management application.
 * Registers API metadata and a JWT Bearer security scheme so that all
 * operations in the generated spec reference it by default.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fleetOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Fleet Management API")
                .version("1.0.0")
                .description("REST API for managing fleet users, drivers, and authentication."))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
