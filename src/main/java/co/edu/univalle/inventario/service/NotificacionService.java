package co.edu.univalle.inventario.service;

import co.edu.univalle.inventario.client.NotificacionesClient;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.Notificacion;
import co.edu.univalle.inventario.repository.NotificacionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Notificación de stock bajo con outbox transaccional (HU_CUTIT_17; DAS v1.3, Tabla 5 — Tolerancia a fallos):
 * <ol>
 *   <li>se guarda Pendiente dentro de la transacción de la salida;</li>
 *   <li>después del commit, en otro hilo, se envía a la API de Notificaciones;</li>
 *   <li>si el envío falla, sigue Pendiente y la salida no se revierte.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    /** Evento publicado en la transacción de la salida; se entrega solo si la transacción hace commit. */
    public record StockBajoDetectado(List<Long> notificacionIds) {
    }

    private final NotificacionRepository notificacionRepository;
    private final NotificacionesClient notificacionesClient;
    private final JsonMapper jsonMapper;

    /**
     * Guarda la notificación de stock bajo dentro de la transacción de la salida.
     * @param articulo artículo que pasó a stock bajo
     * @param referencia referencia del pedido
     * @return id de la notificación
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long crearPendiente(Articulo articulo, String referencia) {
        BigDecimal existencias = normalizar(articulo.getExistencias());
        BigDecimal umbral = normalizar(articulo.getUmbralMinimo());
        BigDecimal faltante = normalizar(articulo.getUmbralMinimo().subtract(articulo.getExistencias()));

        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("rolDestinatario", "SUPERVISOR_PRODUCCION");
        contenido.put("codigo", articulo.getCodigo());
        contenido.put("nombre", articulo.getNombre());
        contenido.put("existencias", existencias);
        contenido.put("umbralMinimo", umbral);
        contenido.put("faltante", faltante);
        contenido.put("origenSalida", "Pedido");
        contenido.put("referencia", referencia);
        contenido.put("mensaje", "Stock bajo: el artículo " + articulo.getCodigo() + " – " + articulo.getNombre()
                + " quedó con " + existencias.toPlainString() + " y su umbral mínimo es " + umbral.toPlainString()
                + ". Faltante: " + faltante.toPlainString());

        Notificacion notificacion = Notificacion.pendiente(articulo, jsonMapper.writeValueAsString(contenido),
                Instant.now());
        return notificacionRepository.save(notificacion).getId();
    }

    /**
     * Envía la notificación después del commit; si falla queda PENDIENTE.
     * @param evento evento con el id de la notificación
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enviar(StockBajoDetectado evento) {
        for (Long id : evento.notificacionIds()) {
            Notificacion notificacion = notificacionRepository.findById(id).orElseThrow();
            try {
                notificacionesClient.enviar(id, notificacion.getContenido());
                notificacion.marcarEnviada(Instant.now());
            } catch (RestClientException e) {
                notificacion.registrarFallo(Instant.now());
                log.warn("No se pudo enviar la notificación {}: {}", id, e.getMessage());
            }
        }
    }

    private static BigDecimal normalizar(BigDecimal valor) {
        return new BigDecimal(valor.stripTrailingZeros().toPlainString());
    }
}
