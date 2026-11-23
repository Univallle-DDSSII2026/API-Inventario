package co.edu.univalle.inventario.model;
import lombok.Getter;

@Getter
public enum TipoArticulo {
    MATERIA_PRIMA("MP", "Materia prima"),
    PRODUCTO_TERMINADO("PT", "Producto terminado"),
    ARTICULO_TERCEROS("AT", "Artículo de terceros");

    /** Prefijo del código de los artículos de este tipo (por ejemplo, PT) y clave de su consecutivo. */
    private final String prefijo;

    /** Nombre del tipo tal como aparece en los mensajes de la API (por ejemplo, "Producto terminado"). */
    private final String etiqueta;

    TipoArticulo(String prefijo, String etiqueta) {
        this.prefijo = prefijo;
        this.etiqueta = etiqueta;
    }
}