package co.edu.univalle.inventario.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import co.edu.univalle.inventario.exception.SolicitudInvalidaException;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.TipoArticulo;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Prueba los criterios 2 a 4 de HU_CUTIT_06 en el Validator, con el mensaje exacto de cada uno. No necesita base de
 * datos: estas validaciones solo dependen del artículo y del texto recibido.
 */
class ArticuloValidatorTest {

    private final ArticuloValidator validator = new ArticuloValidator(mock(ArticuloRepository.class));

    private static Articulo articulo(TipoArticulo tipo, String codigo, String unidadMedida) {
        return Articulo.nuevo(tipo, codigo, "Artículo de prueba", null, unidadMedida);
    }

    @Test
    void deberia_exigir_el_codigo_del_articulo() {
        assertThatThrownBy(() -> validator.validarObligatorios(null, "5"))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El campo Código del artículo es obligatorio");
        assertThatThrownBy(() -> validator.validarObligatorios("  ", "5"))
                .hasMessage("El campo Código del artículo es obligatorio");
    }

    @Test
    void deberia_exigir_el_umbral_minimo() {
        assertThatThrownBy(() -> validator.validarObligatorios("PT-00001", null))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El campo Umbral mínimo es obligatorio");
        assertThatThrownBy(() -> validator.validarObligatorios("PT-00001", "  "))
                .hasMessage("El campo Umbral mínimo es obligatorio");
    }

    @Test
    void deberia_pedir_primero_el_codigo_si_faltan_los_dos_campos() {
        assertThatThrownBy(() -> validator.validarObligatorios("", null))
                .hasMessage("El campo Código del artículo es obligatorio");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT-0001", "PT-000011", "XX-00001", "pt-00001", "PT00001", "PT-0000A", "MP_00001"})
    void deberia_rechazar_un_codigo_que_no_tiene_el_formato(String codigo) {
        assertThatThrownBy(() -> validator.validarFormatoCodigo(codigo))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El Código del artículo no tiene el formato XX-00000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT-00001", "MP-00001", "AT-99999"})
    void deberia_aceptar_un_codigo_con_el_formato(String codigo) {
        assertThatCode(() -> validator.validarFormatoCodigo(codigo)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1e3", "5,5", "--5", ".5", "5.", "+5", "5 5", "NaN"})
    void deberia_rechazar_un_umbral_que_no_es_numerico(String umbral) {
        Articulo producto = articulo(TipoArticulo.PRODUCTO_TERMINADO, "PT-00001", null);

        assertThatThrownBy(() -> validator.validarUmbral(producto, umbral))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El Umbral mínimo debe ser un valor numérico");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "-0.5", "-100"})
    void deberia_rechazar_un_umbral_menor_que_0(String umbral) {
        Articulo producto = articulo(TipoArticulo.PRODUCTO_TERMINADO, "PT-00001", null);

        assertThatThrownBy(() -> validator.validarUmbral(producto, umbral))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El Umbral mínimo debe ser mayor o igual a 0");
    }

    @Test
    void deberia_rechazar_decimales_en_producto_terminado_articulo_de_terceros_y_materia_prima_und() {
        Articulo producto = articulo(TipoArticulo.PRODUCTO_TERMINADO, "PT-00001", null);
        Articulo terceros = articulo(TipoArticulo.ARTICULO_TERCEROS, "AT-00001", null);
        Articulo materiaUnidades = articulo(TipoArticulo.MATERIA_PRIMA, "MP-00002", "und");

        assertThatThrownBy(() -> validator.validarUmbral(producto, "2.5"))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El Umbral mínimo del artículo PT-00001 debe ser un número entero");
        assertThatThrownBy(() -> validator.validarUmbral(terceros, "0.01"))
                .hasMessage("El Umbral mínimo del artículo AT-00001 debe ser un número entero");
        assertThatThrownBy(() -> validator.validarUmbral(materiaUnidades, "7.1"))
                .hasMessage("El Umbral mínimo del artículo MP-00002 debe ser un número entero");
    }

    @Test
    void deberia_rechazar_mas_de_2_decimales_en_materia_prima_con_otra_unidad() {
        Articulo materia = articulo(TipoArticulo.MATERIA_PRIMA, "MP-00001", "kg");

        assertThatThrownBy(() -> validator.validarUmbral(materia, "12.555"))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El Umbral mínimo admite máximo 2 decimales");
    }

    @Test
    void deberia_aceptar_enteros_decimales_permitidos_y_ceros_a_la_derecha() {
        Articulo producto = articulo(TipoArticulo.PRODUCTO_TERMINADO, "PT-00001", null);
        Articulo materia = articulo(TipoArticulo.MATERIA_PRIMA, "MP-00001", "kg");

        assertThat(validator.validarUmbral(producto, "0")).isEqualByComparingTo("0");
        assertThat(validator.validarUmbral(producto, "5.00")).isEqualByComparingTo("5");
        assertThat(materia.umbralEsEntero()).isFalse();
        assertThat(validator.validarUmbral(materia, "12.5")).isEqualByComparingTo(new BigDecimal("12.5"));
        assertThat(validator.validarUmbral(materia, "12.55")).isEqualByComparingTo(new BigDecimal("12.55"));
        assertThat(validator.validarUmbral(materia, "12.500")).isEqualByComparingTo(new BigDecimal("12.5"));
    }

    @Test
    void deberia_aceptar_un_umbral_muy_grande_porque_no_tiene_valor_maximo() {
        Articulo producto = articulo(TipoArticulo.PRODUCTO_TERMINADO, "PT-00001", null);

        assertThat(validator.validarUmbral(producto, "999999999")).isEqualByComparingTo("999999999");
    }
}