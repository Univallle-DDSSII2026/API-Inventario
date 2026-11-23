package co.edu.univalle.inventario.model;

import lombok.Getter;

/**
 * Estado de existencias de un artículo (HU_CUTIT_06, criterio 6). No se guarda: se calcula al consultar.
 */
@Getter
public enum EstadoExistencias {
    STOCK_BAJO("Stock bajo"),
    NORMAL("Normal");

    /** Texto que muestra la API (por ejemplo, "Stock bajo"). */
    private final String etiqueta;

    EstadoExistencias(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}