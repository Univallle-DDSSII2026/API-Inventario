package co.edu.univalle.inventario.validator;

import co.edu.univalle.inventario.exception.NombreDuplicadoException;
import co.edu.univalle.inventario.exception.SolicitudInvalidaException;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validator de la Logic Layer: comprueba las reglas de negocio que dependen de los datos guardados (DAS v1.3,
 * Figura 5).
 */
@Component
@RequiredArgsConstructor
public class ArticuloValidator {

    private static final Pattern FORMATO_CODIGO = Pattern.compile("(MP|PT|AT)-[0-9]{5}");
    private static final Pattern FORMATO_NUMERO = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");
    private static final int MAX_DECIMALES_MATERIA_PRIMA = 2;

    private final ArticuloRepository articuloRepository;

    /**
     * Verifica que ningún artículo, de cualquier tipo, tenga ya ese nombre (sin importar mayúsculas).
     *
     * @param nombre nombre ya ajustado
     * @throws NombreDuplicadoException si el nombre ya está en uso
     */
    public void validarNombre(String nombre) {
        if (articuloRepository.existsByNombreIgnoreCase(nombre)) {
            throw new NombreDuplicadoException(nombre);
        }
    }

    /**
     * Criterio 2 de HU_CUTIT_06: el Código del artículo y el Umbral mínimo son obligatorios.
     *
     * @param codigo Código del artículo recibido, sin espacios al inicio ni al final
     * @param umbralMinimo Umbral mínimo recibido como texto
     * @throws SolicitudInvalidaException si alguno de los dos está vacío
     */
    public void validarObligatorios(String codigo, String umbralMinimo) {
        if (estaVacio(codigo)) {
            throw new SolicitudInvalidaException("El campo Código del artículo es obligatorio");
        }
        if (estaVacio(umbralMinimo)) {
            throw new SolicitudInvalidaException("El campo Umbral mínimo es obligatorio");
        }
    }

    /**
     * Criterio 3 de HU_CUTIT_06 (primera parte): el Código debe tener el formato XX-00000 con prefijo MP, PT o AT.
     *
     * @param codigo Código del artículo recibido
     * @throws SolicitudInvalidaException si el formato no es válido
     */
    public void validarFormatoCodigo(String codigo) {
        if (!FORMATO_CODIGO.matcher(codigo).matches()) {
            throw new SolicitudInvalidaException("El Código del artículo no tiene el formato XX-00000");
        }
    }

    /**
     * Criterio 4 de HU_CUTIT_06: valida el Umbral mínimo contra el tipo y la unidad de medida del artículo.
     *
     * @param articulo artículo existente cuyo umbral se va a definir
     * @param umbralMinimo Umbral mínimo recibido como texto, ya sin espacios
     * @return el Umbral mínimo como número, listo para guardar
     * @throws SolicitudInvalidaException con el mensaje del subcriterio 4.1, 4.2 o 4.3 que se incumpla
     */
    public BigDecimal validarUmbral(Articulo articulo, String umbralMinimo) {
        if (!FORMATO_NUMERO.matcher(umbralMinimo).matches()) {
            throw new SolicitudInvalidaException("El Umbral mínimo debe ser un valor numérico");
        }
        BigDecimal umbral = new BigDecimal(umbralMinimo);
        if (umbral.signum() < 0) {
            throw new SolicitudInvalidaException("El Umbral mínimo debe ser mayor o igual a 0");
        }
        // 5.0 y 5.00 valen lo mismo que 5: se cuentan solo los decimales con valor.
        int decimales = Math.max(umbral.stripTrailingZeros().scale(), 0);
        if (articulo.umbralEsEntero() && decimales > 0) {
            throw new SolicitudInvalidaException(
                    "El Umbral mínimo del artículo %s debe ser un número entero".formatted(articulo.getCodigo()));
        }
        if (!articulo.umbralEsEntero() && decimales > MAX_DECIMALES_MATERIA_PRIMA) {
            throw new SolicitudInvalidaException("El Umbral mínimo admite máximo 2 decimales");
        }
        return umbral;
    }

    private static boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}