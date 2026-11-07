package co.edu.univalle.inventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.univalle.inventario.dto.SalidaPedidoRequest;
import co.edu.univalle.inventario.dto.SalidaPedidoRequest.Linea;
import co.edu.univalle.inventario.dto.SalidaResponse;
import co.edu.univalle.inventario.exception.SolicitudRechazadaException;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.Movimiento;
import co.edu.univalle.inventario.model.TipoArticulo;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import co.edu.univalle.inventario.repository.MovimientoRepository;
import co.edu.univalle.inventario.validator.ValidadorSalida;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

    @Mock
    private ArticuloRepository articuloRepository;
    @Mock
    private MovimientoRepository movimientoRepository;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private ApplicationEventPublisher eventos;

    private MovimientoService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new MovimientoService(articuloRepository, movimientoRepository, new ValidadorSalida(),
                notificacionService, eventos);
    }

    private static Articulo productoTerminado(String codigo, int existencias, int umbral) {
        return new Articulo(codigo, "Artículo " + codigo, TipoArticulo.PRODUCTO_TERMINADO,
                BigDecimal.valueOf(existencias), BigDecimal.valueOf(umbral));
    }

    private static SalidaPedidoRequest solicitud(Linea... lineas) {
        return new SalidaPedidoRequest("PED-42", LocalDate.of(2026, 11, 7), List.of(lineas));
    }

    @Test
    void deberia_registrar_las_validas_rechazar_las_invalidas_y_notificar_el_stock_bajo() {
        // Arrange
        Articulo cadena = productoTerminado("PT-00001", 10, 5);
        Articulo cuchilla = productoTerminado("PT-00002", 3, 0);
        when(articuloRepository.bloquearPorCodigos(anyCollection())).thenReturn(List.of(cadena, cuchilla));
        when(notificacionService.crearPendiente(cadena, "PED-42")).thenReturn(77L);

        // Act
        SalidaResponse respuesta = servicio.registrarSalida(solicitud(new Linea("PT-00001", 6),
                new Linea("PT-00002", 1), new Linea("PT-00099", 1)));

        // Assert
        assertThat(respuesta.mensaje())
                .isEqualTo("Salida registrada: 2 línea(s) registrada(s) y 1 línea(s) rechazada(s)");
        assertThat(respuesta.lineasRechazadas())
                .containsExactly("Línea 3: no existe un artículo con el código PT-00099");
        assertThat(cadena.getExistencias()).isEqualByComparingTo("4");
        assertThat(cuchilla.getExistencias()).isEqualByComparingTo("2");
        verify(movimientoRepository, times(2)).save(any(Movimiento.class));
        verify(eventos).publishEvent(new NotificacionService.StockBajoDetectado(List.of(77L)));
    }

    @Test
    void deberia_no_notificar_si_el_articulo_ya_estaba_en_stock_bajo() {
        // Arrange
        when(articuloRepository.bloquearPorCodigos(anyCollection()))
                .thenReturn(List.of(productoTerminado("PT-00001", 4, 5)));

        // Act
        servicio.registrarSalida(solicitud(new Linea("PT-00001", 1)));

        // Assert
        verify(notificacionService, never()).crearPendiente(any(), any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void deberia_rechazar_toda_la_solicitud_sin_guardar_nada_si_todas_las_lineas_fallan() {
        // Act & Assert
        assertThatThrownBy(() -> servicio.registrarSalida(solicitud(new Linea("PT-1", 1))))
                .isInstanceOf(SolicitudRechazadaException.class)
                .hasMessage("No se registró ninguna salida. Revise las líneas rechazadas");
        verify(articuloRepository, never()).bloquearPorCodigos(anyCollection());
        verify(movimientoRepository, never()).save(any());
    }
}
