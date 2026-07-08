package de.codefingers.validata.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 Configuration für Swagger UI
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI fraudLensOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FraudLens API")
                        .version("1.0.0")
                        .description("Kfz-Werkstattrechnungs-Betrugserkennungs-API\n\n" +
                                "Automatische Betrugserkennung für Werkstattabrechnungen mit:\n" +
                                "- Bedrock AI Analysis (Layer 1)\n" +
                                "- Hallucination Removal (Layer 2)\n" +
                                "- Labor Price Validation (Layer 3)\n" +
                                "- Parts Price Validation (Layer 4)\n" +
                                "- Vehicle History Validation (Layer 5)\n" +
                                "- Duplication Detection (Layer 6)")
                        .termsOfService("https://example.com/terms")
                        .contact(new Contact()
                                .name("FraudLens Support")
                                .url("https://example.com")
                                .email("support@fraudlens.com"))
                        .license(new License()
                                .name("Proprietary License")
                                .url("https://example.com/license")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer Token for authentication")));
    }
}