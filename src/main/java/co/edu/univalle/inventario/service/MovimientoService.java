package co.edu.univalle.inventario.service;

import co.edu.univalle.inventario.dto.SalidaPedidoRequest;
import co.edu.univalle.inventario.dto.SalidaResponse;
import co.edu.univalle.inventario.exception.SolicitudRechazadaException;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.Movimiento;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import co.edu.univalle.inventario.repository.MovimientoRepository;
import co.edu.univalle.inventario.validator.ValidadorSalida;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de la salida por pedido de productos terminados: el proceso crítico de la Figura 6 del DAS.
 *
 * <p>Una sola transacción ACID por solicitud: bloquea los artículos, valida cada línea, registra solo las válidas
 * y guarda la notificación de stock bajo en la misma transacción. Si todas las líneas fallan, no se guarda nada.</p>
 */
@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final ArticuloRepository articuloRepository;
    private final MovimientoRepository movimientoRepository;
    private final ValidadorSalida validador;
    private final NotificacionService notificacionService;
    private final ApplicationEventPublisher eventos;

    /**
     * Registra en una sola transacción las líneas válidas de la salida y descuenta existencias.
     * @param solicitud datos de la salida
     * @return resultado con líneas registradas y rechazadas
     * @throws SolicitudRechazadaException si ninguna línea es válida
     */
    @Transactional
    public SalidaResponse registrarSalida(SalidaPedidoRequest solicitud) {
        Map<String, Articulo> articulos = bloquear(validador.codigosABloquear(solicitud.lineas()));
        ValidadorSalida.Resultado resultado = validador.validar(solicitud.lineas(), articulos);
        if (resultado.aceptadas().isEmpty()) {
            throw new SolicitudRechazadaException("No se registró ninguna salida. Revise las líneas rechazadas",
                    resultado.rechazadas());
        }

        String referencia = solicitud.referenciaPedido().trim();
        UUID solicitudId = UUID.randomUUID();
        Instant ahora = Instant.now();
        List<Long> notificaciones = new ArrayList<>();
        for (ValidadorSalida.LineaAceptada linea : resultado.aceptadas()) {
            Articulo articulo = linea.articulo();
            boolean estabaNormal = !articulo.enStockBajo();
            articulo.restar(linea.cantidad());
            movimientoRepository.save(Movimiento.salidaPorPedido(articulo, linea.cantidad(), solicitud.fecha(),
                    referencia, solicitudId, ahora));
            // HU_CUTIT_17: una notificación por artículo que pasa de «Normal» a «Stock bajo».
            if (estabaNormal && articulo.enStockBajo()) {
                notificaciones.add(notificacionService.crearPendiente(articulo, referencia));
            }
        }
        if (!notificaciones.isEmpty()) {
            eventos.publishEvent(new NotificacionService.StockBajoDetectado(notificaciones));
        }

        int registradas = resultado.aceptadas().size();
        String mensaje = "Salida registrada: " + registradas + " línea(s) registrada(s) y "
                + resultado.rechazadas().size() + " línea(s) rechazada(s)";
        return new SalidaResponse(solicitudId, mensaje, registradas, resultado.rechazadas());
    }

    private Map<String, Articulo> bloquear(Set<String> codigos) {
        Map<String, Articulo> articulos = new HashMap<>();
        if (!codigos.isEmpty()) {
            for (Articulo articulo : articuloRepository.bloquearPorCodigos(codigos)) {
                articulos.put(articulo.getCodigo(), articulo);
            }
        }
        return articulos;
    }
}
