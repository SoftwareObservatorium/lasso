package de.uni_mannheim.swt.lasso.sheets.service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Marcus Kessel
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Sheets Service API", version = "1.0",
                contact = @Contact(name = "Marcus Kessel", email = "marcus.kessel@uni-mannheim.de", url = "https://www.wim.uni-mannheim.de/atkinson/"),
                license = @License(name = "TODO", url = "https://xxx.xxx"), termsOfService = "XXX",
                description = "LASSO Service API"),
        servers = {
                @Server(url = "http://localhost:8877", description = "Development")
         })
public class SwaggerConfig {

    @Bean
    public OpenAPI customizeOpenAPI() {
        final String securitySchemeName = "Authorization";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description(
                                        "Provide the JWT token. JWT token can be obtained from the SingIn API.")
                                .bearerFormat("JWT")));
    }
}
