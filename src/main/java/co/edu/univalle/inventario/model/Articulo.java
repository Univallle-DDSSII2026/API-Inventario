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

    @NotNull
    @Column(nullable = false, precision = 14, scale = 2)
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
        Articulo articulo = new Articulo();
        articulo.codigo = codigo;
        articulo.nombre = nombre;
        articulo.descripcion = descripcion;
        articulo.tipo = tipo;
        articulo.existencias = BigDecimal.ZERO;
        articulo.umbralMinimo = BigDecimal.ZERO;
        return articulo;
    }
}