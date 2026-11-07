package co.edu.univalle.inventario.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Solicitud de salida por pedido (View de MVC; HU_CUTIT_04, criterios 1 y 3 a 6).
 *
 * <p>El DTO valida solo el encabezado. Las líneas no llevan restricciones: las valida el ValidadorSalida, porque
 * una línea con error rechaza solo esa línea (DAS v1.3, sección 4, «Dónde se valida»).</p>
 */
public record SalidaPedidoRequest(
        @Schema(example = "PED-2026-0042")
        @NotBlank(message = "El campo Referencia del pedido es obligatorio")
        @Size(max = 30, message = "El campo Referencia del pedido no puede superar 30 caracteres")
        String referenciaPedido,

        @Schema(type = "string", example = "07/11/2026", description = "Formato DD/MM/AAAA")
        @JsonFormat(pattern = "dd/MM/yyyy")
        @NotNull(message = "El campo Fecha es obligatorio")
        @PastOrPresent(message = "La Fecha no puede ser posterior a la fecha actual")
        LocalDate fecha,

        @NotEmpty(message = "Debe incluir al menos un artículo")
        @Size(max = 50, message = "La solicitud supera el máximo de 50 líneas")
        List<Linea> lineas) {

    /**
     * Línea de la solicitud: código del artículo y cantidad.
     */
    public record Linea(
            @Schema(example = "PT-00001") String codigoArticulo,
            @Schema(example = "6") Integer cantidad) {
    }
}
