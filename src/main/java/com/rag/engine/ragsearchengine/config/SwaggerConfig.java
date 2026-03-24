package com.rag.engine.ragsearchengine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RAG Search Engine API")
                        .description("REST API for document ingestion and retrieval-augmented generation")
                        .version("1.0.0"));
    }
}
