package co.edu.univalle.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Notificación de stock bajo, guardada en la misma transacción de la salida (outbox; DAS v1.3, Tabla 2).
 */
@Entity
@Table(name = "notificacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "articulo_id", nullable = false)
    private Articulo articulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoNotificacion estado;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(name = "fecha_creacion", nullable = false, columnDefinition = "timestamptz")
    private Instant fechaCreacion;

    @Column(name = "fecha_ultimo_intento", columnDefinition = "timestamptz")
    private Instant fechaUltimoIntento;

    @Column(name = "fecha_envio", columnDefinition = "timestamptz")
    private Instant fechaEnvio;

    /**
     * Crea una notificación pendiente de envío (outbox).
     * @param articulo artículo en stock bajo
     * @param contenido cuerpo JSON a enviar
     * @param ahora fecha de creación
     * @return la notificación pendiente
     */
    public static Notificacion pendiente(Articulo articulo, String contenido, Instant ahora) {
        Notificacion notificacion = new Notificacion();
        notificacion.articulo = articulo;
        notificacion.contenido = contenido;
        notificacion.estado = EstadoNotificacion.PENDIENTE;
        notificacion.fechaCreacion = ahora;
        return notificacion;
    }

    /**
     * Marca la notificación como enviada.
     * @param ahora fecha de envío
     */
    public void marcarEnviada(Instant ahora) {
        estado = EstadoNotificacion.ENVIADA;
        fechaUltimoIntento = ahora;
        fechaEnvio = ahora;
    }

    /** El envío falló: la notificación sigue Pendiente y la salida no se revierte (HU_CUTIT_17, criterio 8). */
    public void registrarFallo(Instant ahora) {
        fechaUltimoIntento = ahora;
    }
}
