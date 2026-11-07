package co.edu.univalle.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoArticulo tipo;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal existencias;

    @Column(name = "umbral_minimo", nullable = false, precision = 14, scale = 2)
    private BigDecimal umbralMinimo;

    /**
     * Crea un artículo (usado por pruebas y datos de ejemplo).
     * @param codigo código único
     * @param nombre nombre del artículo
     * @param tipo tipo de artículo
     * @param existencias existencias iniciales
     * @param umbralMinimo umbral de stock bajo
     */
    public Articulo(String codigo, String nombre, TipoArticulo tipo, BigDecimal existencias,
                    BigDecimal umbralMinimo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.existencias = existencias;
        this.umbralMinimo = umbralMinimo;
    }

    /** «Stock bajo»: existencias estrictamente menores que el umbral mínimo (HU_CUTIT_06, criterio 6). */
    public boolean enStockBajo() {
        return existencias.compareTo(umbralMinimo) < 0;
    }

    /**
     * Indica si las existencias cubren la cantidad pedida.
     * @param cantidad cantidad a descontar
     * @return true si hay existencias suficientes
     */
    public boolean alcanzaPara(BigDecimal cantidad) {
        return cantidad.compareTo(existencias) <= 0;
    }

    /**
     * Descuenta la cantidad de las existencias.
     * @param cantidad cantidad a descontar
     */
    public void restar(BigDecimal cantidad) {
        if (!alcanzaPara(cantidad)) {
            throw new IllegalStateException("La salida supera las existencias del artículo " + codigo);
        }
        existencias = existencias.subtract(cantidad);
    }
}
