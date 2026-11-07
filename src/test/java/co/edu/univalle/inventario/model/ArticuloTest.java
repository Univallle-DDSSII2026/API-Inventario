package co.edu.univalle.inventario.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ArticuloTest {

    private static Articulo cadena(int existencias, int umbral) {
        return new Articulo("PT-00001", "Cadena Ripit 5000", TipoArticulo.PRODUCTO_TERMINADO,
                BigDecimal.valueOf(existencias), BigDecimal.valueOf(umbral));
    }

    @Test
    void deberia_estar_en_stock_bajo_solo_si_las_existencias_son_menores_que_el_umbral() {
        // Act & Assert
        assertThat(cadena(4, 5).enStockBajo()).isTrue();
        assertThat(cadena(5, 5).enStockBajo()).isFalse();
    }

    @Test
    void deberia_restar_una_salida_que_alcanza() {
        // Arrange
        Articulo articulo = cadena(10, 5);

        // Act
        articulo.restar(BigDecimal.valueOf(6));

        // Assert
        assertThat(articulo.getExistencias()).isEqualByComparingTo("4");
    }

    @Test
    void deberia_impedir_una_salida_que_deje_existencias_negativas() {
        // Arrange
        Articulo articulo = cadena(3, 0);

        // Act & Assert
        assertThatThrownBy(() -> articulo.restar(BigDecimal.valueOf(4)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PT-00001");
        assertThat(articulo.getExistencias()).isEqualByComparingTo("3");
    }
}
