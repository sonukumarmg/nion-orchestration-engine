package com.ainions.nion.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "NION Orchestration API",
                version = "v1",
                description = "Enterprise orchestration engine for AI program management"
        )
)
public class OpenApiConfig {
}
