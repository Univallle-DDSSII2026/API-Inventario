package co.edu.univalle.inventario.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Prueba el prefijo del código y la etiqueta de cada tipo de artículo: los prefijos deben coincidir con los
 * consecutivos de la base de datos (V3) y las etiquetas con los mensajes de los criterios de aceptación.
 */
class TipoArticuloTest {

    @Test
    void deberia_asignar_a_cada_tipo_el_prefijo_de_su_consecutivo() {
        assertThat(TipoArticulo.MATERIA_PRIMA.getPrefijo()).isEqualTo("MP");
        assertThat(TipoArticulo.PRODUCTO_TERMINADO.getPrefijo()).isEqualTo("PT");
        assertThat(TipoArticulo.ARTICULO_TERCEROS.getPrefijo()).isEqualTo("AT");
    }

    @Test
    void deberia_asignar_a_cada_tipo_la_etiqueta_de_los_mensajes() {
        assertThat(TipoArticulo.MATERIA_PRIMA.getEtiqueta()).isEqualTo("Materia prima");
        assertThat(TipoArticulo.PRODUCTO_TERMINADO.getEtiqueta()).isEqualTo("Producto terminado");
        assertThat(TipoArticulo.ARTICULO_TERCEROS.getEtiqueta()).isEqualTo("Artículo de terceros");
    }
}