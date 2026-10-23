package com.tronai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 * Access Swagger UI at: http://localhost:8080/swagger-ui/index.html
 * Access raw JSON at:   http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tronAiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TRON AI — Spring Boot Backend")
                        .description("Step 1B Foundation. FastAPI legacy endpoints will be migrated here incrementally.")
                        .version("0.1.0-SNAPSHOT"));
    }
}
