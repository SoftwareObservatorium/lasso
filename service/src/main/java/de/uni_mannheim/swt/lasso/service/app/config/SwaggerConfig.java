/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.service.app.config;

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
        info = @Info(title = "LASSO Service API", version = "1.0",
                contact = @Contact(name = "Marcus Kessel", email = "marcus.kessel@uni-mannheim.de", url = "https://www.wim.uni-mannheim.de/atkinson/"),
                license = @License(name = "TODO", url = "https://xxx.xxx"), termsOfService = "XXX",
                description = "LASSO Service API"),
        servers = {
                @Server(url = "http://localhost:10222", description = "Quickstart"),
                @Server(url = "http://localhost:8899", description = "Development"),
                @Server(url = "http://lassohp1.informatik.uni-mannheim.de:10222", description = "Test LASSOHP1 Grid"),
                @Server(url = "http://swt100.informatik.uni-mannheim.de:10222", description = "Test SWT100 Grid")

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
