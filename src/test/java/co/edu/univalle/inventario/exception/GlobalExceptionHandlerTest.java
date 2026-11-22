package co.edu.univalle.inventario.exception;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.univalle.inventario.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/**
 * Prueba del GlobalExceptionHandler en las respuestas que no se provocan fácilmente por HTTP: el 500 genérico y los
 * errores que Spring ya clasifica con su propio estado (Tabla 11 del DAS).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deberia_responder_500_con_mensaje_generico_sin_exponer_el_detalle_del_error() {
        // Act
        ResponseEntity<ErrorResponse> respuesta =
                handler.manejarInesperada(new IllegalStateException("detalle interno"));

        // Assert
        assertThat(respuesta.getStatusCode().value()).isEqualTo(500);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().codigo()).isEqualTo(500);
        assertThat(respuesta.getBody().mensaje()).isEqualTo("Ocurrió un error inesperado. Intente de nuevo más tarde.");
    }

    @Test
    void deberia_conservar_el_estado_de_los_errores_que_spring_ya_clasifica() {
        // Act
        ResponseEntity<ErrorResponse> respuesta =
                handler.manejarInesperada(new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Assert
        assertThat(respuesta.getStatusCode().value()).isEqualTo(404);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().mensaje()).isEqualTo("La solicitud no se pudo procesar");
    }
}
