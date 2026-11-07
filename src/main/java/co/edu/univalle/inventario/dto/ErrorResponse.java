package co.edu.univalle.inventario.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Cuerpo de las respuestas de error (DAS v1.3, Tabla 11).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(int codigo, String mensaje, List<String> lineasRechazadas) {

    /**
     * Crea una respuesta de error sin líneas rechazadas.
     * @param codigo código HTTP
     * @param mensaje mensaje para el usuario
     * @return la respuesta de error
     */
    public static ErrorResponse de(int codigo, String mensaje) {
        return new ErrorResponse(codigo, mensaje, List.of());
    }
}
