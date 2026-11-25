package co.edu.univalle.inventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.univalle.inventario.command.RegistrarIngresoCompraTercerosCommand;
import co.edu.univalle.inventario.exception.ArticuloNoEncontradoException;
import co.edu.univalle.inventario.exception.SolicitudInvalidaException;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.Movimiento;
import co.edu.univalle.inventario.model.TipoArticulo;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import co.edu.univalle.inventario.repository.MovimientoRepository;
import co.edu.univalle.inventario.validator.ArticuloValidator;
import co.edu.univalle.inventario.validator.IngresoCompraTercerosValidator;
import co.edu.univalle.inventario.validator.IngresoFabricacionValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IngresoCompraTercerosServiceTest {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ArticuloRepository articuloRepository;
    private MovimientoRepository movimientoRepository;
    private IngresoCompraTercerosService servicio;
    private String fechaHoy;

    @BeforeEach
    void preparar() {
        articuloRepository = mock(ArticuloRepository.class);
        movimientoRepository = mock(MovimientoRepository.class);
        ArticuloValidator articuloValidator = new ArticuloValidator(articuloRepository);
        IngresoFabricacionValidator fabricacionValidator = new IngresoFabricacionValidator(articuloValidator);
        IngresoCompraTercerosValidator validator = new IngresoCompraTercerosValidator(fabricacionValidator);
        servicio = new IngresoCompraTercerosService(articuloRepository, movimientoRepository, validator);
        fechaHoy = LocalDate.now(ZoneId.of("America/Bogota")).format(FORMATO);
    }

    @Test
    void deberia_registrar_movimiento_de_compra_e_incrementar_existencias() {
        Articulo articulo = Articulo.nuevo(
                TipoArticulo.ARTICULO_TERCEROS, "AT-00001", "Motosierra Ripit X", null);
        articulo.incrementarExistencias(new BigDecimal("4.00"));
        when(articuloRepository.findBloqueadoByCodigo("AT-00001")).thenReturn(Optional.of(articulo));

        var command = new RegistrarIngresoCompraTercerosCommand(
                "AT-00001", "6", fechaHoy, "OC-2026-01", "Importadora Norte", "850000.50");

        String mensaje = servicio.registrar(command);

        assertThat(mensaje)
            .isEqualTo("Ingreso registrado: 6 de AT-00001 – Motosierra Ripit X. Existencias actuales: 10");
        assertThat(articulo.getExistencias()).isEqualByComparingTo("10");

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository).save(captor.capture());
        Movimiento guardado = captor.getValue();
        assertThat(guardado.getArticulo()).isSameAs(articulo);
        assertThat(guardado.getTipo()).isEqualTo("Ingreso");
        assertThat(guardado.getOrigen()).isEqualTo("Compra");
        assertThat(guardado.getCantidad()).isEqualByComparingTo("6");
        assertThat(guardado.getReferencia()).isEqualTo("OC-2026-01");
        assertThat(guardado.getProveedor()).isEqualTo("Importadora Norte");
        assertThat(guardado.getCostoUnitario()).isEqualByComparingTo("850000.50");
        assertThat(guardado.getSolicitudId()).isNotNull();
        assertThat(guardado.getFechaRegistro()).isNotNull();
    }

    @Test
    void deberia_lanzar_404_cuando_el_articulo_no_existe() {
        when(articuloRepository.findBloqueadoByCodigo("AT-99999")).thenReturn(Optional.empty());

        var command = new RegistrarIngresoCompraTercerosCommand(
                "AT-99999", "2", fechaHoy, "FAC-1", "Proveedor", "10000");

        assertThatThrownBy(() -> servicio.registrar(command))
                .isInstanceOf(ArticuloNoEncontradoException.class)
                .hasMessage("No existe un artículo con el código AT-99999");
        verify(movimientoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deberia_rechazar_si_el_articulo_no_es_de_terceros() {
        Articulo producto = Articulo.nuevo(TipoArticulo.PRODUCTO_TERMINADO, "PT-00001", "Cadena", null);
        when(articuloRepository.findBloqueadoByCodigo("PT-00001")).thenReturn(Optional.of(producto));

        var command = new RegistrarIngresoCompraTercerosCommand(
                "PT-00001", "2", fechaHoy, "FAC-1", "Proveedor", "10000");

        assertThatThrownBy(() -> servicio.registrar(command))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El artículo PT-00001 no es un artículo de terceros");
        verify(movimientoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
