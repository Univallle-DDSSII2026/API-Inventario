package co.edu.univalle.inventario.exception;

import co.edu.univalle.inventario.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handler Exception de la View Layer: convierte las excepciones en las respuestas HTTP de la Tabla 11 del DAS.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final int UNPROCESSABLE_CONTENT = 422;

    /**
     * Responde 400 con el primer mensaje de validación del cuerpo.
     * @param excepcion errores de Bean Validation
     * @return respuesta 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException excepcion) {
        String mensaje = excepcion.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(MessageSourceResolvable::getDefaultMessage)
                .orElse("La solicitud no es válida");
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    /**
     * Responde 400 cuando el JSON no se puede leer (fecha o cantidad con formato inválido).
     * @param excepcion error de lectura del cuerpo
     * @return respuesta 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoInvalido(HttpMessageNotReadableException excepcion) {
        return responder(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido. Revise que la Fecha tenga el formato DD/MM/AAAA "
                        + "y que la Cantidad sea un número entero.");
    }

    /**
     * Responde 422 cuando ninguna línea de la salida fue aceptada.
     * @param excepcion rechazo con el detalle por línea
     * @return respuesta 422
     */
    @ExceptionHandler(SolicitudRechazadaException.class)
    public ResponseEntity<ErrorResponse> manejarSolicitudRechazada(SolicitudRechazadaException excepcion) {
        return ResponseEntity.status(UNPROCESSABLE_CONTENT).body(
                new ErrorResponse(UNPROCESSABLE_CONTENT, excepcion.getMessage(), excepcion.getLineasRechazadas()));
    }

    /**
     * Responde con el estado de Spring o 500 sin exponer detalles internos.
     * @param excepcion error no controlado
     * @return respuesta de error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarInesperada(Exception excepcion) {
        if (excepcion instanceof org.springframework.web.ErrorResponse errorSpring) {
            return responder(errorSpring.getStatusCode(), "La solicitud no se pudo procesar");
        }
        log.error("Error inesperado", excepcion);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intente de nuevo más tarde.");
    }

    private static ResponseEntity<ErrorResponse> responder(HttpStatusCode estado, String mensaje) {
        return ResponseEntity.status(estado).body(ErrorResponse.de(estado.value(), mensaje));
    }
}
