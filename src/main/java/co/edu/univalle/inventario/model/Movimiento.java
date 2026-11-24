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

/** Movimiento de inventario almacenado en la tabla definida por Flyway. */
@Entity
@Table(name = "movimiento")
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

    @Column(nullable = false, columnDefinition = "numeric")
    private BigDecimal cantidad;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 30)
    private String referencia;

    @Column(name = "solicitud_id", nullable = false)
    private UUID solicitudId;

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;

    /**
     * Crea un ingreso de fabricación con su propio identificador de solicitud.
     * @param articulo producto terminado que recibe el ingreso
     * @param cantidad unidades fabricadas
     * @param fecha fecha de fabricación
     * @param referencia referencia de fabricación
     * @return movimiento listo para persistir
     */
    public static Movimiento ingresoFabricacion(Articulo articulo, BigDecimal cantidad, LocalDate fecha,
            String referencia) {
        Movimiento movimiento = new Movimiento();
        movimiento.articulo = articulo;
        movimiento.tipo = "Ingreso";
        movimiento.origen = "Fabricación";
        movimiento.cantidad = cantidad;
        movimiento.fecha = fecha;
        movimiento.referencia = referencia;
        movimiento.solicitudId = UUID.randomUUID();
        movimiento.fechaRegistro = Instant.now();
        return movimiento;
    }
}
