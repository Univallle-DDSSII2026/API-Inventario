package co.edu.univalle.inventario.config;

import co.edu.univalle.inventario.filter.SecurityFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI con el esquema de API Key: agrega el botón «Authorize» en Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "apiKey";

    @Bean
    OpenAPI documentacion() {
        return new OpenAPI()
                .info(new Info().title("API de Inventario (Equipo 4)").version("0.1.0"))
                .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(SecurityFilter.ENCABEZADO)))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
