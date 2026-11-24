package co.edu.univalle.inventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.univalle.inventario.command.RegistrarIngresoFabricacionCommand;
import co.edu.univalle.inventario.exception.ArticuloNoEncontradoException;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.Movimiento;
import co.edu.univalle.inventario.model.TipoArticulo;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import co.edu.univalle.inventario.repository.MovimientoRepository;
import co.edu.univalle.inventario.validator.ArticuloValidator;
import co.edu.univalle.inventario.validator.IngresoFabricacionValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

class IngresoFabricacionServiceTest {

    private final ArticuloRepository articulos = mock(ArticuloRepository.class);
    private final MovimientoRepository movimientos = mock(MovimientoRepository.class);
    private final IngresoFabricacionService servicio = new IngresoFabricacionService(
            articulos, movimientos, new IngresoFabricacionValidator(new ArticuloValidator(articulos)));

    @Test
    void deberia_sumar_el_saldo_y_construir_el_mensaje_exacto() {
        Articulo articulo = Articulo.nuevo(TipoArticulo.PRODUCTO_TERMINADO, "PT-00001", "Cadena", null);
        articulo.incrementarExistencias(new BigDecimal("10.00"));
        when(articulos.findBloqueadoByCodigo(articulo.getCodigo())).thenReturn(Optional.of(articulo));

        String mensaje = servicio.registrar(comando(articulo.getCodigo()));

        assertThat(mensaje).isEqualTo("Ingreso registrado: 2 de PT-00001 – Cadena. Existencias actuales: 12");
        assertThat(articulo.getExistencias()).isEqualByComparingTo("12");
        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientos).save(captor.capture());
        assertThat(captor.getValue().getArticulo()).isSameAs(articulo);
        assertThat(captor.getValue().getCantidad()).isEqualByComparingTo("2");
    }

    @Test
    void deberia_rechazar_el_articulo_inexistente() {
        assertThatThrownBy(() -> servicio.registrar(comando("PT-99999")))
                .isInstanceOf(ArticuloNoEncontradoException.class)
                .hasMessage("No existe un artículo con el código PT-99999");
        verifyNoInteractions(movimientos);
    }

    @ParameterizedTest
    @EnumSource(value = TipoArticulo.class, names = {"MATERIA_PRIMA", "ARTICULO_TERCEROS"})
    void deberia_rechazar_un_tipo_ajeno_a_fabricacion(TipoArticulo tipo) {
        String codigo = tipo.getPrefijo() + "-00001";
        Articulo articulo = Articulo.nuevo(tipo, codigo, "Insumo", null);
        when(articulos.findBloqueadoByCodigo(codigo)).thenReturn(Optional.of(articulo));

        assertThatThrownBy(() -> servicio.registrar(comando(codigo)))
                .hasMessage("El artículo %s no es un producto terminado", codigo);
        assertThat(articulo.getExistencias()).isZero();
        verifyNoInteractions(movimientos);
    }

    private RegistrarIngresoFabricacionCommand comando(String codigo) {
        String hoy = LocalDate.now(ZoneId.of("America/Bogota")).format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
        return new RegistrarIngresoFabricacionCommand(codigo, "2", hoy, "LOTE");
    }
}
