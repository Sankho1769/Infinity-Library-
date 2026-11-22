package com.librarymanagement.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI infinityLibraryOpenAPI() {
        final String securitySchemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Infinity Library Management System API")
                        .description("Production-grade, highly reliable Library Management REST API engineered with " +
                                "Java 25, Spring Boot 4.2.x, PostgreSQL 19/18, Flyway, and RFC 9457 standards. " +
                                "Incorporates pessimistic locking for concurrency control, object-level authorization, " +
                                "and Argon2id password hashing.\n\n" +
                                "**Design Identity Visual Palette:**\n" +
                                "- **Primary Teal**: `#2A5344` (Authority, Identity, Headers)\n" +
                                "- **Deep Navy**: `#0B3048` (Typography, High Contrast Accents)\n" +
                                "- **Neon Lime**: `#B9F12F` (Interactive Focus, Action Badges)\n" +
                                "- **Warm Off White**: `#F2F0E9` (Background Canvas, Cards)")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Engineering Directorate")
                                .email("engineering@infinitylibrary.org")
                                .url("https://github.com/Sankho1769/Infinity-Library-"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token (obtained via `/api/v1/auth/login`)")));
    }
}
