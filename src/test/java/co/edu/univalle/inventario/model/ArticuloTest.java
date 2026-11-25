package co.edu.univalle.inventario.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Prueba las reglas de HU_CUTIT_06 que viven en el Model: qué artículos exigen un umbral entero, cómo se guarda el
 * umbral y el cálculo del Estado de existencias (criterio 6).
 */
class ArticuloTest {

    private static Articulo articulo(TipoArticulo tipo, String unidadMedida, String existencias, String umbral) {
        Articulo articulo = Articulo.nuevo(tipo, "XX-00010", "Artículo de prueba", null, unidadMedida);
        ReflectionTestUtils.setField(articulo, "existencias", new BigDecimal(existencias));
        articulo.definirUmbral(new BigDecimal(umbral));
        return articulo;
    }

    @Test
    void deberia_exigir_umbral_entero_en_productos_terminados_articulos_de_terceros_y_materias_primas_und() {
        assertThat(articulo(TipoArticulo.PRODUCTO_TERMINADO, null, "0", "0").umbralEsEntero()).isTrue();
        assertThat(articulo(TipoArticulo.ARTICULO_TERCEROS, null, "0", "0").umbralEsEntero()).isTrue();
        assertThat(articulo(TipoArticulo.MATERIA_PRIMA, "und", "0", "0").umbralEsEntero()).isTrue();
        assertThat(articulo(TipoArticulo.MATERIA_PRIMA, "UND", "0", "0").umbralEsEntero()).isTrue();
    }

    @Test
    void deberia_admitir_decimales_en_materias_primas_con_otra_unidad_de_medida() {
        assertThat(articulo(TipoArticulo.MATERIA_PRIMA, "kg", "0", "0").umbralEsEntero()).isFalse();
        assertThat(articulo(TipoArticulo.MATERIA_PRIMA, null, "0", "0").umbralEsEntero()).isFalse();
    }

    @Test
    void deberia_guardar_el_umbral_sin_decimales_si_es_entero_y_con_2_decimales_en_las_demas_materias_primas() {
        assertThat(articulo(TipoArticulo.PRODUCTO_TERMINADO, null, "0", "5.00").getUmbralMinimo().toPlainString())
                .isEqualTo("5");
        assertThat(articulo(TipoArticulo.MATERIA_PRIMA, "kg", "0", "12.5").getUmbralMinimo().toPlainString())
                .isEqualTo("12.50");
    }

    @Test
    void deberia_quedar_en_stock_bajo_solo_si_las_existencias_son_estrictamente_menores_que_el_umbral() {
        assertThat(articulo(TipoArticulo.PRODUCTO_TERMINADO, null, "3", "5").estadoExistencias())
                .isEqualTo(EstadoExistencias.STOCK_BAJO);
        assertThat(articulo(TipoArticulo.MATERIA_PRIMA, "kg", "4.99", "5").estadoExistencias())
                .isEqualTo(EstadoExistencias.STOCK_BAJO);
    }

    @Test
    void deberia_quedar_normal_si_las_existencias_son_iguales_o_mayores_que_el_umbral() {
        assertThat(articulo(TipoArticulo.PRODUCTO_TERMINADO, null, "5", "5").estadoExistencias())
                .isEqualTo(EstadoExistencias.NORMAL);
        assertThat(articulo(TipoArticulo.PRODUCTO_TERMINADO, null, "6", "5").estadoExistencias())
                .isEqualTo(EstadoExistencias.NORMAL);
        assertThat(articulo(TipoArticulo.ARTICULO_TERCEROS, null, "0", "0").estadoExistencias())
                .isEqualTo(EstadoExistencias.NORMAL);
    }

    @Test
    void deberia_mostrar_la_etiqueta_del_estado_tal_como_la_pide_el_criterio_6() {
        assertThat(EstadoExistencias.STOCK_BAJO.getEtiqueta()).isEqualTo("Stock bajo");
        assertThat(EstadoExistencias.NORMAL.getEtiqueta()).isEqualTo("Normal");
    }
}