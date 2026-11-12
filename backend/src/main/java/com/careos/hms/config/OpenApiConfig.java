package com.careos.hms.config;

import com.careos.hms.common.constants.ApiConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI careOsOpenApi(
            @Value("${app.version:0.1.0-SNAPSHOT}") String appVersion
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title("CareOS-HMS API")
                        .version(appVersion)
                        .description("Hospital Management System REST API v1 Foundation")
                        .contact(new Contact().name("CareOS Engineering")))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Short-lived JWT access token")
                ));
    }

    @Bean
    public GroupedOpenApi v1ApiGroup() {
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch(ApiConstants.API_V1_BASE_PATH + "/**")
                .build();
    }
}
