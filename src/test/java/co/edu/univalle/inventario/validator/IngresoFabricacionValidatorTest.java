package co.edu.univalle.inventario.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import co.edu.univalle.inventario.exception.SolicitudInvalidaException;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IngresoFabricacionValidatorTest {

    private static final LocalDate HOY = LocalDate.of(2024, 3, 30);

    private final IngresoFabricacionValidator validador = new IngresoFabricacionValidator(
            new ArticuloValidator(mock(ArticuloRepository.class)));

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void deberia_exigir_cada_campo(String valor) {
        assertThatThrownBy(() -> validador.validarCodigo(valor))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El campo Código del artículo es obligatorio");
        assertThatThrownBy(() -> validador.validarCantidad(valor)).hasMessage("El campo Cantidad es obligatorio");
        assertThatThrownBy(() -> validador.validarFecha(valor, HOY)).hasMessage("El campo Fecha es obligatorio");
        assertThatThrownBy(() -> validador.validarReferencia(valor))
                .hasMessage("El campo Referencia de fabricación es obligatorio");
    }

    @Test
    void deberia_rechazar_un_codigo_invalido() {
        assertThatThrownBy(() -> validador.validarCodigo("XX-00001"))
                .hasMessage("El Código del artículo no tiene el formato XX-00000");
    }

    @ParameterizedTest
    @CsvSource({
        "abc, La Cantidad debe ser un valor numérico",
        "1.00000000000000000001, La Cantidad debe ser un número entero",
        "0, La Cantidad debe ser mayor o igual a 1",
        "-1, La Cantidad debe ser mayor o igual a 1"
    })
    void deberia_distinguir_errores_de_cantidad(String cantidad, String mensaje) {
        assertThatThrownBy(() -> validador.validarCantidad(cantidad)).hasMessage(mensaje);
    }

    @Test
    void deberia_aceptar_cantidades_enteras_sin_maximo_en_el_validador() {
        assertThat(validador.validarCantidad("1")).isEqualByComparingTo("1");
        assertThat(validador.validarCantidad("3.0")).isEqualByComparingTo("3");
        assertThat(validador.validarCantidad("100000000000000000000")).isEqualByComparingTo("100000000000000000000");
    }

    @Test
    void deberia_comprobar_la_escala_real_enviada_a_jdbc() {
        validador.validarRepresentabilidad(new BigDecimal("1e-16383"));
        assertThatThrownBy(() -> validador.validarRepresentabilidad(new BigDecimal("1e-16384")))
                .hasMessage("La cantidad o las existencias resultantes exceden la capacidad de almacenamiento exacto");
    }

    @ParameterizedTest
    @CsvSource({
        "2024-03-30, La Fecha no tiene el formato DD/MM/AAAA",
        "30/3/2024, La Fecha no tiene el formato DD/MM/AAAA",
        "31/02/2024, La Fecha no tiene el formato DD/MM/AAAA",
        "29/02/2023, La Fecha no tiene el formato DD/MM/AAAA",
        "31/03/2024, La Fecha no puede ser posterior a la fecha actual",
        "28/02/2024, La Fecha no puede tener más de 30 días de antigüedad"
    })
    void deberia_distinguir_errores_de_fecha(String fecha, String mensaje) {
        assertThatThrownBy(() -> validador.validarFecha(fecha, HOY)).hasMessage(mensaje);
    }

    @Test
    void deberia_aceptar_hoy_y_exactamente_treinta_dias_incluido_el_dia_bisiesto() {
        assertThat(validador.validarFecha("30/03/2024", HOY)).isEqualTo(HOY);
        assertThat(validador.validarFecha("29/02/2024", HOY)).isEqualTo(HOY.minusDays(30));
    }

    @Test
    void deberia_preservar_la_referencia_y_medir_su_longitud_en_caracteres() {
        String referencia = " " + "ñ".repeat(28) + " ";
        assertThat(validador.validarReferencia(referencia)).isEqualTo(referencia);
        assertThat(validador.validarReferencia("✓")).isEqualTo("✓");
        assertThatThrownBy(() -> validador.validarReferencia(referencia + "!"))
                .hasMessage("El campo Referencia de fabricación no puede superar 30 caracteres");
    }
}
