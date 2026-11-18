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
}