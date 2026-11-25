package co.edu.univalle.inventario.dto;

import co.edu.univalle.inventario.model.Articulo;

/**
 * Respuesta de una definición de umbral exitosa (View Layer): el mensaje del criterio 5 y el artículo actualizado.
 *
 * @param mensaje mensaje de confirmación
 * @param articulo artículo con el nuevo Umbral mínimo y su Estado de existencias
 */
public record UmbralActualizadoDto(String mensaje, ArticuloDto articulo) {

    /**
     * Arma la respuesta del criterio 5 de HU_CUTIT_06.
     *
     * @param articulo artículo ya actualizado
     * @return la respuesta con el mensaje "Umbral mínimo de [Código] actualizado a [Umbral mínimo]"
     */
    public static UmbralActualizadoDto de(Articulo articulo) {
        String mensaje = "Umbral mínimo de %s actualizado a %s"
                .formatted(articulo.getCodigo(), articulo.getUmbralMinimo().toPlainString());
        return new UmbralActualizadoDto(mensaje, ArticuloDto.de(articulo));
    }
}