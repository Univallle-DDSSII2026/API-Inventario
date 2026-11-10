package co.edu.univalle.inventario.filter;

import co.edu.univalle.inventario.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Escribe desde los filtros el mismo cuerpo de error que el GlobalExceptionHandler (DAS v1.3, Tabla 11).
 */
public class RespuestaError {

    private final JsonMapper jsonMapper;

    /**
     * Crea el escritor de respuestas de error.
     * @param jsonMapper conversor JSON de Spring
     */
    public RespuestaError(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Escribe la respuesta de error.
     * @param respuesta respuesta HTTP
     * @param codigo código HTTP
     * @param mensaje mensaje para el cliente
     * @throws IOException si no se puede escribir la respuesta
     */
    public void escribir(HttpServletResponse respuesta, int codigo, String mensaje) throws IOException {
        respuesta.setStatus(codigo);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write(jsonMapper.writeValueAsString(ErrorResponse.de(codigo, mensaje)));
    }
}
