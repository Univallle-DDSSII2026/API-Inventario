package co.edu.univalle.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Registro inmutable de un ingreso o una salida (tabla de auditoría; DAS v1.3, Tabla 5 — No repudio).
 */
@Entity
@Table(name = "movimiento")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "articulo_id", nullable = false)
    private Articulo articulo;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false, length = 12)
    private String origen;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal cantidad;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 30)
    private String referencia;

    @Column(name = "solicitud_id", nullable = false)
    private UUID solicitudId;

    @Column(name = "fecha_registro", nullable = false, columnDefinition = "timestamptz")
    private Instant fechaRegistro;

    /** Salida con origen «Pedido» (HU_CUTIT_04, criterio 8). */
    public static Movimiento salidaPorPedido(Articulo articulo, BigDecimal cantidad, LocalDate fecha,
                                             String referencia, UUID solicitudId, Instant fechaRegistro) {
        Movimiento movimiento = new Movimiento();
        movimiento.articulo = articulo;
        movimiento.tipo = "SALIDA";
        movimiento.origen = "PEDIDO";
        movimiento.cantidad = cantidad;
        movimiento.fecha = fecha;
        movimiento.referencia = referencia;
        movimiento.solicitudId = solicitudId;
        movimiento.fechaRegistro = fechaRegistro;
        return movimiento;
    }
}
