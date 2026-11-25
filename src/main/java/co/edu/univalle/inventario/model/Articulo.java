package co.edu.univalle.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Artículo cuyas existencias controla Inventario (Model de MVC, Data Access Layer; DAS v1.3, Figura 5).
 */
@Entity
@Table(name = "articulo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Articulo {

    private static final String UNIDAD_ENTERA = "und";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true, length = 8)
    private String codigo;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nombre;

    @Size(max = 500)
    @Column(length = 500)
    private String descripcion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoArticulo tipo;

    /** Unidad de medida (por ejemplo, kg o und). Solo la tienen las materias primas; en los demás tipos es null. */
    @Size(max = 10)
    @Column(name = "unidad_medida", length = 10)
    private String unidadMedida;

    @NotNull
    @Column(nullable = false, columnDefinition = "numeric")
    private BigDecimal existencias;

    @NotNull
    @Column(name = "umbral_minimo", nullable = false, precision = 14, scale = 2)
    private BigDecimal umbralMinimo;

    /**
     * Crea un artículo nuevo del tipo indicado, con Existencias 0 y Umbral mínimo 0 (HU_CUTIT_01 y HU_CUTIT_14).
     * @param tipo tipo del artículo
     * @param codigo código generado (por ejemplo, PT-00003)
     * @param nombre nombre ya normalizado
     * @param descripcion descripción ya normalizada, o null si no se envió
     * @return el artículo listo para guardar
     */
    public static Articulo nuevo(TipoArticulo tipo, String codigo, String nombre, String descripcion) {
        return nuevo(tipo, codigo, nombre, descripcion, null);
    }

    /**
     * Crea un artículo nuevo con Unidad de medida (la usan las materias primas), con Existencias 0 y Umbral mínimo 0.
     * @param tipo tipo del artículo
     * @param codigo código generado (por ejemplo, MP-00002)
     * @param nombre nombre ya normalizado
     * @param descripcion descripción ya normalizada, o null si no se envió
     * @param unidadMedida unidad de medida (por ejemplo, kg o und), o null si el tipo no la tiene
     * @return el artículo listo para guardar
     */
    public static Articulo nuevo(TipoArticulo tipo, String codigo, String nombre, String descripcion,
            String unidadMedida) {
        Articulo articulo = new Articulo();
        articulo.codigo = codigo;
        articulo.nombre = nombre;
        articulo.descripcion = descripcion;
        articulo.tipo = tipo;
        articulo.unidadMedida = unidadMedida;
        articulo.existencias = BigDecimal.ZERO;
        articulo.umbralMinimo = BigDecimal.ZERO;
        return articulo;
    }

    /**
     * Indica si el Umbral mínimo de este artículo debe ser un número entero: productos terminados, artículos de
     * terceros y materias primas con Unidad de medida "und" (HU_CUTIT_06, criterio 1). Las demás materias primas
     * admiten hasta 2 decimales.
     * @return true si el umbral no admite decimales
     */
    public boolean umbralEsEntero() {
        return tipo != TipoArticulo.MATERIA_PRIMA || UNIDAD_ENTERA.equalsIgnoreCase(unidadMedida);
    }

    /**
     * Reemplaza el Umbral mínimo (HU_CUTIT_06, criterio 5). El valor ya debe estar validado: se guarda con 0 decimales
     * si el artículo es entero y con 2 en las demás materias primas.
     * @param umbral nuevo Umbral mínimo ya validado
     */
    public void definirUmbral(BigDecimal umbral) {
        this.umbralMinimo = umbral.setScale(umbralEsEntero() ? 0 : 2, RoundingMode.UNNECESSARY);
    }

    /**
     * Calcula el Estado de existencias (HU_CUTIT_06, criterio 6): "Stock bajo" si las Existencias son estrictamente
     * menores que el Umbral mínimo y "Normal" si son mayores o iguales.
     * @return el estado calculado con los valores actuales
     */
    public EstadoExistencias estadoExistencias() {
        return existencias.compareTo(umbralMinimo) < 0 ? EstadoExistencias.STOCK_BAJO : EstadoExistencias.NORMAL;
    }

    /**
     * Suma unidades fabricadas ya validadas a las existencias.
     * @param cantidad unidades que ingresan al inventario
     */
    public void incrementarExistencias(BigDecimal cantidad) {
        existencias = existencias.add(cantidad);
    }
}
