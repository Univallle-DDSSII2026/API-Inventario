package co.edu.univalle.inventario.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.univalle.inventario.dto.ErrorResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler manejador = new GlobalExceptionHandler();

    private static MethodArgumentNotValidException validacionCon(String... mensajes) {
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "solicitud");
        for (String mensaje : mensajes) {
            resultado.addError(new FieldError("solicitud", "campo", mensaje));
        }
        MethodArgumentNotValidException excepcion = mock(MethodArgumentNotValidException.class);
        when(excepcion.getBindingResult()).thenReturn(resultado);
        return excepcion;
    }

    @Test
    void deberia_responder_400_con_el_mensaje_de_la_validacion() {
        // Act
        ResponseEntity<ErrorResponse> respuesta = manejador.manejarValidacion(
                validacionCon("El campo Fecha es obligatorio"));

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().mensaje()).isEqualTo("El campo Fecha es obligatorio");
    }

    @Test
    void deberia_responder_400_generico_si_la_validacion_no_trae_mensaje() {
        // Act
        ResponseEntity<ErrorResponse> respuesta = manejador.manejarValidacion(validacionCon());

        // Assert
        assertThat(respuesta.getBody().mensaje()).isEqualTo("La solicitud no es válida");
    }

    @Test
    void deberia_responder_400_si_el_json_no_es_valido() {
        // Act
        ResponseEntity<ErrorResponse> respuesta = manejador.manejarCuerpoInvalido(
                mock(HttpMessageNotReadableException.class));

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().mensaje()).contains("DD/MM/AAAA");
    }

    @Test
    void deberia_responder_422_con_las_lineas_rechazadas() {
        // Act
        ResponseEntity<ErrorResponse> respuesta = manejador.manejarSolicitudRechazada(
                new SolicitudRechazadaException("No se registró ninguna salida. Revise las líneas rechazadas",
                        List.of("Línea 1: el campo Cantidad es obligatorio")));

        // Assert
        assertThat(respuesta.getStatusCode().value()).isEqualTo(422);
        assertThat(respuesta.getBody().lineasRechazadas()).containsExactly("Línea 1: el campo Cantidad es obligatorio");
    }

    @Test
    void deberia_conservar_el_codigo_de_las_excepciones_de_spring() {
        // Act
        ResponseEntity<ErrorResponse> respuesta = manejador.manejarInesperada(
                new ErrorResponseException(HttpStatus.NOT_FOUND));

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deberia_responder_500_sin_detalles_internos() {
        // Act
        ResponseEntity<ErrorResponse> respuesta = manejador.manejarInesperada(new IllegalStateException("secreto"));

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody().mensaje()).doesNotContain("secreto");
    }
}
