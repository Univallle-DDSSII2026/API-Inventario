package co.edu.univalle.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cuerpo de {@code PUT /inventario/articulos/{codigo}/umbral} (View Layer, HU_CUTIT_06). El Umbral mínimo llega como
 * texto para poder responder "El Umbral mínimo debe ser un valor numérico" cuando no es un número; el Validator lo
 * convierte y aplica los criterios 2 a 4. Acepta tanto {@code 10} como {@code "10"} en el JSON.
 *
 * @param umbralMinimo Umbral mínimo recibido, sin espacios al inicio ni al final
 */
public record DefinirUmbralDto(@Schema(type = "number", example = "10") String umbralMinimo) {

    /**
     * Quita los espacios al inicio y al final apenas se crea el DTO.
     *
     * @param umbralMinimo Umbral mínimo recibido
     */
    public DefinirUmbralDto {
        umbralMinimo = umbralMinimo == null ? null : umbralMinimo.strip();
    }
}