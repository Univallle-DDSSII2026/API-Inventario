package co.edu.univalle.inventario.config;

import co.edu.univalle.inventario.filter.ClavesApi;
import co.edu.univalle.inventario.filter.RateLimitFilter;
import co.edu.univalle.inventario.filter.RespuestaError;
import co.edu.univalle.inventario.filter.SecurityFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cadena de filtros de la View Layer: RateLimitFilter → SecurityFilter → RestController (DAS v1.3, Figura 4).
 * El RBAC por rol de la API Key se aplica con @PreAuthorize en los controladores.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Documentación y health check: se consultan sin API Key. */
    private static final String[] RUTAS_PUBLICAS = {
        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health"
    };

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, JsonMapper jsonMapper,
                                          @Value("${inventario.seguridad.api-keys}") String apiKeys,
                                          @Value("${inventario.seguridad.limite-por-minuto}") long limitePorMinuto)
            throws Exception {
        RespuestaError respuestaError = new RespuestaError(jsonMapper);
        SecurityFilter securityFilter = new SecurityFilter(ClavesApi.desde(apiKeys), respuestaError);
        RateLimitFilter rateLimitFilter = new RateLimitFilter(limitePorMinuto, respuestaError);

        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errores -> errores.authenticationEntryPoint((peticion, respuesta, excepcion) ->
                        respuestaError.escribir(respuesta, 401, SecurityFilter.MENSAJE_NO_AUTORIZADO)))
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, SecurityFilter.class);
        return http.build();
    }
}
