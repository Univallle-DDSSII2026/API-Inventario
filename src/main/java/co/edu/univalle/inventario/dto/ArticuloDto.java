package co.edu.univalle.inventario.dto;

import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.TipoArticulo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Datos de un artículo que la API entrega al cliente (View de MVC; DAS v1.3, Figura 5).
 * Así la API no expone la entidad del Model.
 */
public record ArticuloDto(
        @Schema(example = "PT-00001") String codigo,
        @Schema(example = "Cadena Ripit 5000") String nombre,
        TipoArticulo tipo,
        BigDecimal existencias,
        BigDecimal umbralMinimo) {

    /**
     * Convierte la entidad en el DTO que se entrega al cliente.
     * @param articulo entidad del Model
     * @return el DTO
     */
    public static ArticuloDto de(Articulo articulo) {
        return new ArticuloDto(articulo.getCodigo(), articulo.getNombre(), articulo.getTipo(),
                articulo.getExistencias(), articulo.getUmbralMinimo());
    }
}