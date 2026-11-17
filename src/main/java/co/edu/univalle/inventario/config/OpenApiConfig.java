package co.edu.univalle.inventario.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI de la API: lo publica Swagger UI (Utility Layer).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI documentacion() {
        return new OpenAPI().info(new Info().title("API de Inventario (Equipo 4)").version("0.1.0"));
    }
}