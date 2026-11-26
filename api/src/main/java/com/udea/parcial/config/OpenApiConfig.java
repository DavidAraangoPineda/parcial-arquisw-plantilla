package com.udea.parcial.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI en /swagger-ui.html, con un grupo por versión de la API. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI informacion() {
        return new OpenAPI().info(new Info()
                .title("Parcial Arquitectura de Software - API")
                .description("API RESTful con HATEOAS (HAL), versionamiento por URI y observabilidad.")
                .version("v1, v2"));
    }

    @Bean
    GroupedOpenApi v1() {
        return GroupedOpenApi.builder().group("v1").pathsToMatch("/api/v1/**").build();
    }

    @Bean
    GroupedOpenApi v2() {
        return GroupedOpenApi.builder().group("v2").pathsToMatch("/api/v2/**").build();
    }
}
