package com.github.simohin.file.storage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for OpenAPI/Swagger documentation
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("File Storage Service API")
                        .version("1.0.0")
                        .description(
                                """
                                        RESTful API for file storage, management, and sharing service. \
                                        This service allows users to upload files with metadata, \
                                        manage visibility settings, organize files with tags, \
                                        and download files with secure access control.
                                        """)
                        .contact(new Contact()
                                .name("File Storage API Support")
                                .email("support@file-storage.com")
                                .url("https://github.com/simohin/file-storage"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server")
                ));
    }
}