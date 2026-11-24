package co.edu.univalle.inventario.validator;

import co.edu.univalle.inventario.exception.SolicitudInvalidaException;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.TipoArticulo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Valida los campos y las reglas de ingreso de fabricación. */
@Component
@RequiredArgsConstructor
public class IngresoFabricacionValidator {

    private static final Pattern FORMATO_FECHA = Pattern.compile("[0-9]{2}/[0-9]{2}/[0-9]{4}");
    private static final DateTimeFormatter FECHA_ESTRICTA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final String ERROR_FECHA = "La Fecha no tiene el formato DD/MM/AAAA";

    // NUMERIC sin precisión declarada y peso binario de 16 bits en grupos de cuatro dígitos.
    private static final int MAX_DIGITOS_ENTEROS = 131_072;
    private static final int MAX_DIGITOS_DECIMALES = 16_383;
    private static final String ERROR_CAPACIDAD =
            "La cantidad o las existencias resultantes exceden la capacidad de almacenamiento exacto";

    private final ArticuloValidator articuloValidator;

    /** Valida el código obligatorio y su formato. */
    public String validarCodigo(Object valor) {
        exigirCampo(valor, "Código del artículo");
        String codigo = valor instanceof String texto ? texto.strip() : "";
        articuloValidator.validarFormatoCodigo(codigo);
        return codigo;
    }

    /** Exige que el artículo existente sea un producto terminado. */
    public void validarTipo(Articulo articulo) {
        if (articulo.getTipo() != TipoArticulo.PRODUCTO_TERMINADO) {
            throw new SolicitudInvalidaException(
                    "El artículo %s no es un producto terminado".formatted(articulo.getCodigo()));
        }
    }

    /** Convierte la cantidad a un número entero positivo de precisión exacta. */
    public BigDecimal validarCantidad(String valor) {
        exigirCampo(valor, "Cantidad");
        BigDecimal cantidad;
        try {
            cantidad = new BigDecimal(valor.strip());
        } catch (NumberFormatException excepcion) {
            throw new SolicitudInvalidaException("La Cantidad debe ser un valor numérico");
        }
        // Con escala negativa el valor ya es entero. Quitar ceros allí podría desbordar la escala de Java.
        if (cantidad.scale() > 0 && cantidad.signum() != 0) {
            cantidad = cantidad.stripTrailingZeros();
        }
        if (cantidad.scale() > 0 && cantidad.signum() != 0) {
            throw new SolicitudInvalidaException("La Cantidad debe ser un número entero");
        }
        if (cantidad.signum() <= 0) {
            throw new SolicitudInvalidaException("La Cantidad debe ser mayor o igual a 1");
        }
        return cantidad;
    }

    /** Comprueba la escala y la magnitud del valor que se enviará a JDBC, sin expandir el exponente. */
    public void validarRepresentabilidad(BigDecimal valor) {
        int escala = valor.scale();
        long digitosEnteros = valor.signum() == 0 ? 0 : (long) valor.precision() - escala;
        if (digitosEnteros > MAX_DIGITOS_ENTEROS || escala > MAX_DIGITOS_DECIMALES) {
            throw new SolicitudInvalidaException(ERROR_CAPACIDAD);
        }
    }

    /** Valida una fecha real dentro del intervalo inclusivo de los últimos 30 días. */
    public LocalDate validarFecha(Object valor, LocalDate hoy) {
        exigirCampo(valor, "Fecha");
        if (!(valor instanceof String texto) || !FORMATO_FECHA.matcher(texto).matches()) {
            throw new SolicitudInvalidaException(ERROR_FECHA);
        }
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(texto, FECHA_ESTRICTA);
        } catch (DateTimeParseException excepcion) {
            throw new SolicitudInvalidaException(ERROR_FECHA);
        }
        if (fecha.getYear() == 0) {
            throw new SolicitudInvalidaException(ERROR_FECHA);
        }
        if (fecha.isAfter(hoy)) {
            throw new SolicitudInvalidaException("La Fecha no puede ser posterior a la fecha actual");
        }
        if (fecha.isBefore(hoy.minusDays(30))) {
            throw new SolicitudInvalidaException("La Fecha no puede tener más de 30 días de antigüedad");
        }
        return fecha;
    }

    /** Valida la longitud sin cambiar el texto suministrado. */
    public String validarReferencia(Object valor) {
        exigirCampo(valor, "Referencia de fabricación");
        if (!(valor instanceof String referencia)) {
            throw new SolicitudInvalidaException("El campo Referencia de fabricación debe ser texto");
        }
        if (referencia.codePointCount(0, referencia.length()) > 30) {
            throw new SolicitudInvalidaException("El campo Referencia de fabricación no puede superar 30 caracteres");
        }
        return referencia;
    }

    private void exigirCampo(Object valor, String campo) {
        if (valor == null || valor instanceof String texto && texto.isBlank()) {
            throw new SolicitudInvalidaException("El campo %s es obligatorio".formatted(campo));
        }
    }
}
