package co.edu.univalle.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Último número usado de cada prefijo de código: AT, PT, MP (Model de MVC; DAS v1.3, Figura 5).
 */
@Entity
@Table(name = "consecutivo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consecutivo {

    @Id
    @NotNull
    @Column(length = 2)
    private String prefijo;

    @Column(name = "ultimo_valor", nullable = false)
    private int ultimoValor;

    /**
     * Avanza el consecutivo y arma el código con el formato prefijo, guion y 5 dígitos (por ejemplo, AT-00001).
     * @return el siguiente código disponible
     */
    public String siguiente() {
        ultimoValor++;
        return "%s-%05d".formatted(prefijo, ultimoValor);
    }
}