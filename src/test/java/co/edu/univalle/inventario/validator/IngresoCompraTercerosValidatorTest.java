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
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class IngresoCompraTercerosValidatorTest {

    private IngresoCompraTercerosValidator validator;
    private LocalDate hoy;

    @BeforeEach
    void preparar() {
        ArticuloValidator articuloValidator = new ArticuloValidator(mock(ArticuloRepository.class));
        IngresoFabricacionValidator fabricacionValidator = new IngresoFabricacionValidator(articuloValidator);
        validator = new IngresoCompraTercerosValidator(fabricacionValidator);
        hoy = LocalDate.of(2026, 11, 24);
    }

    @Test
    void deberia_aceptar_campos_obligatorios_completos() {
        assertThatCode(() -> validator.validarObligatorios(
                "AT-00001", "5", "24/11/2026", "FAC-001", "Proveedor", "150000"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
        "codigo, El campo Código del artículo es obligatorio",
        "cantidad, El campo Cantidad es obligatorio",
        "fecha, El campo Fecha es obligatorio",
        "factura, El campo Número de factura u orden de compra es obligatorio",
        "proveedor, El campo Proveedor es obligatorio",
        "costo, El campo Costo unitario es obligatorio"
    })
    void deberia_rechazar_cada_campo_obligatorio_vacio(String campoFaltante, String mensajeEsperado) {
        Object codigo = "codigo".equals(campoFaltante) ? "   " : "AT-00001";
        String cantidad = "cantidad".equals(campoFaltante) ? null : "5";
        Object fecha = "fecha".equals(campoFaltante) ? "" : "24/11/2026";
        Object factura = "factura".equals(campoFaltante) ? null : "FAC-001";
        Object proveedor = "proveedor".equals(campoFaltante) ? " " : "Proveedor";
        String costo = "costo".equals(campoFaltante) ? "" : "150000";

        assertThatThrownBy(() -> validator.validarObligatorios(
                codigo, cantidad, fecha, factura, proveedor, costo))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage(mensajeEsperado);
    }

    @Test
    void deberia_aceptar_articulo_de_terceros_y_rechazar_otros_tipos() {
        Articulo tercero = Articulo.nuevo(TipoArticulo.ARTICULO_TERCEROS, "AT-00001", "Motosierra", null);
        Articulo producto = Articulo.nuevo(TipoArticulo.PRODUCTO_TERMINADO, "PT-00001", "Cadena", null);
        Articulo materia = Articulo.nuevo(TipoArticulo.MATERIA_PRIMA, "MP-00001", "Acero", null, "kg");

        assertThatCode(() -> validator.validarTipo(tercero)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validarTipo(producto))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El artículo PT-00001 no es un artículo de terceros");
        assertThatThrownBy(() -> validator.validarTipo(materia))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El artículo MP-00001 no es un artículo de terceros");
    }

    @Test
    void deberia_validar_codigo_cantidad_fecha_y_representabilidad() {
        assertThat(validator.validarCodigo(" AT-00001 ")).isEqualTo("AT-00001");
        assertThat(validator.validarCantidad("10")).isEqualByComparingTo("10");
        assertThat(validator.validarFecha("24/11/2026", hoy)).isEqualTo(hoy);
        assertThatCode(() -> validator.validarRepresentabilidad(new BigDecimal("1000"))).doesNotThrowAnyException();
    }

    @Test
    void deberia_validar_numero_de_factura_y_proveedor() {
        String factura30 = "A".repeat(30);
        String proveedor100 = "B".repeat(100);

        assertThat(validator.validarNumeroFactura(factura30)).isEqualTo(factura30);
        assertThat(validator.validarProveedor(proveedor100)).isEqualTo(proveedor100);

        assertThatThrownBy(() -> validator.validarNumeroFactura("A".repeat(31)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El campo Número de factura u orden de compra no puede superar 30 caracteres");
        assertThatThrownBy(() -> validator.validarProveedor("B".repeat(101)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El campo Proveedor no puede superar 100 caracteres");
        assertThatThrownBy(() -> validator.validarNumeroFactura(12345))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El campo Número de factura u orden de compra debe ser texto");
        assertThatThrownBy(() -> validator.validarProveedor(999))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El campo Proveedor debe ser texto");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "0.01", "1500000.5", "1500000.50", "1500000.500", " 2500 "})
    void deberia_aceptar_costos_unitarios_validos(String costo) {
        assertThat(validator.validarCostoUnitario(costo)).isPositive();
    }

    @ParameterizedTest
    @CsvSource({
        "abc, El Costo unitario debe ser un valor numérico",
        "12.34.56, El Costo unitario debe ser un valor numérico",
        "0, El Costo unitario debe ser mayor que 0",
        "0.00, El Costo unitario debe ser mayor que 0",
        "-500, El Costo unitario debe ser mayor que 0",
        "100.123, El Costo unitario admite máximo 2 decimales",
        "0.001, El Costo unitario admite máximo 2 decimales"
    })
    void deberia_rechazar_costos_unitarios_invalidos(String costo, String mensajeEsperado) {
        assertThatThrownBy(() -> validator.validarCostoUnitario(costo))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage(mensajeEsperado);
    }

    @Test
    void deberia_rechazar_costo_unitario_vacio() {
        assertThatThrownBy(() -> validator.validarCostoUnitario("   "))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El campo Costo unitario es obligatorio");
    }
}
