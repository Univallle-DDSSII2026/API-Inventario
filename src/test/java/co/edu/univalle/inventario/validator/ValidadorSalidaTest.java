package co.edu.univalle.inventario.validator;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.univalle.inventario.dto.SalidaPedidoRequest.Linea;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.TipoArticulo;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * HU_CUTIT_04, subcriterios 7.1 a 7.5: cada caso verifica el mensaje exacto del backlog.
 */
class ValidadorSalidaTest {

    private final ValidadorSalida validador = new ValidadorSalida();
    private final Articulo cadena = new Articulo("PT-00001", "Cadena Ripit 5000", TipoArticulo.PRODUCTO_TERMINADO,
            new BigDecimal("10.00"), new BigDecimal("5.00"));
    private final Articulo acero = new Articulo("MP-00001", "Acero templado", TipoArticulo.MATERIA_PRIMA,
            new BigDecimal("50.00"), new BigDecimal("10.00"));
    private final Map<String, Articulo> articulos = Map.of("PT-00001", cadena, "MP-00001", acero);

    private String rechazo(Linea linea) {
        ValidadorSalida.Resultado resultado = validador.validar(Arrays.asList(linea), articulos);
        assertThat(resultado.aceptadas()).isEmpty();
        return resultado.rechazadas().get(0);
    }

    @Test
    void deberia_aceptar_una_linea_valida() {
        // Act
        ValidadorSalida.Resultado resultado = validador.validar(List.of(new Linea(" PT-00001 ", 6)), articulos);

        // Assert
        assertThat(resultado.rechazadas()).isEmpty();
        assertThat(resultado.aceptadas().get(0).articulo()).isSameAs(cadena);
        assertThat(resultado.aceptadas().get(0).cantidad()).isEqualByComparingTo("6");
    }

    @Test
    void deberia_rechazar_cada_subcriterio_con_su_mensaje() {
        // Act & Assert
        assertThat(rechazo(null)).isEqualTo("Línea 1: el campo Código del artículo es obligatorio");
        assertThat(rechazo(new Linea(null, 1))).isEqualTo("Línea 1: el campo Código del artículo es obligatorio");
        assertThat(rechazo(new Linea("PT-00001", null))).isEqualTo("Línea 1: el campo Cantidad es obligatorio");
        assertThat(rechazo(new Linea("PT-1", 1)))
                .isEqualTo("Línea 1: el Código del artículo no tiene el formato XX-00000");
        assertThat(rechazo(new Linea("PT-00099", 1)))
                .isEqualTo("Línea 1: no existe un artículo con el código PT-00099");
        assertThat(rechazo(new Linea("MP-00001", 1)))
                .isEqualTo("Línea 1: el artículo MP-00001 no es un producto terminado");
        assertThat(rechazo(new Linea("PT-00001", 0))).isEqualTo("Línea 1: la Cantidad debe ser mayor o igual a 1");
        assertThat(rechazo(new Linea("PT-00001", 11)))
                .isEqualTo("Línea 1: la cantidad solicitada (11) supera las existencias (10) del artículo PT-00001");
    }

    @Test
    void deberia_rechazar_un_articulo_repetido_y_aceptar_su_primera_linea() {
        // Act
        ValidadorSalida.Resultado resultado = validador.validar(
                List.of(new Linea("PT-00001", 2), new Linea("PT-00001", 3)), articulos);

        // Assert
        assertThat(resultado.aceptadas()).hasSize(1);
        assertThat(resultado.rechazadas()).containsExactly("Línea 2: el artículo PT-00001 ya está en la línea 1");
    }

    @Test
    void deberia_bloquear_solo_codigos_validos_sin_repetir_y_en_orden() {
        // Act & Assert
        assertThat(validador.codigosABloquear(Arrays.asList(new Linea("PT-00002", 1), null, new Linea("malo", 1),
                new Linea("PT-00001", 1), new Linea("PT-00001", 2), new Linea(" ", 1))))
                .containsExactly("PT-00001", "PT-00002");
    }
}
