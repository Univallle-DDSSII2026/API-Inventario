package co.edu.univalle.inventario.validator;

import co.edu.univalle.inventario.dto.SalidaPedidoRequest.Linea;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.TipoArticulo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validator de la Logic Layer para las líneas de una salida por pedido de productos terminados
 * (HU_CUTIT_04, subcriterios 7.1 a 7.5, en ese orden; DAS v1.3, Tabla 10).
 */
@Component
public class ValidadorSalida {

    private static final Pattern FORMATO_CODIGO = Pattern.compile("^(MP|PT|AT)-\\d{5}$");

    /** Línea que cumple todos los subcriterios. */
    public record LineaAceptada(Articulo articulo, BigDecimal cantidad) {
    }

    /** Líneas aceptadas y mensajes de las rechazadas. */
    public record Resultado(List<LineaAceptada> aceptadas, List<String> rechazadas) {
    }

    /** Códigos con formato válido, sin repetir y ordenados, para bloquearlos antes de validar existencias. */
    public Set<String> codigosABloquear(List<Linea> lineas) {
        Set<String> codigos = new TreeSet<>();
        for (Linea linea : lineas) {
            String codigo = codigo(linea);
            if (codigo != null && FORMATO_CODIGO.matcher(codigo).matches()) {
                codigos.add(codigo);
            }
        }
        return codigos;
    }

    /**
     * Valida cada línea contra los artículos ya bloqueados (un código ausente del mapa no existe).
     */
    public Resultado validar(List<Linea> lineas, Map<String, Articulo> articulos) {
        List<LineaAceptada> aceptadas = new ArrayList<>();
        List<String> rechazadas = new ArrayList<>();
        Map<String, Integer> primeraLinea = new HashMap<>();

        for (int i = 0; i < lineas.size(); i++) {
            int numero = i + 1;
            String codigo = codigo(lineas.get(i));
            Integer cantidad = lineas.get(i) == null ? null : lineas.get(i).cantidad();
            if (codigo != null) {
                primeraLinea.putIfAbsent(codigo, numero);
            }
            String error = primerError(numero, codigo, cantidad, articulos, primeraLinea);
            if (error == null) {
                aceptadas.add(new LineaAceptada(articulos.get(codigo), BigDecimal.valueOf(cantidad)));
            } else {
                rechazadas.add("Línea " + numero + ": " + error);
            }
        }
        return new Resultado(aceptadas, rechazadas);
    }

    private static String primerError(int numero, String codigo, Integer cantidad, Map<String, Articulo> articulos,
                                      Map<String, Integer> primeraLinea) {
        // 7.1 Campos obligatorios
        if (codigo == null) {
            return "el campo Código del artículo es obligatorio";
        }
        if (cantidad == null) {
            return "el campo Cantidad es obligatorio";
        }
        // 7.2 Formato, existencia y tipo
        if (!FORMATO_CODIGO.matcher(codigo).matches()) {
            return "el Código del artículo no tiene el formato XX-00000";
        }
        Articulo articulo = articulos.get(codigo);
        if (articulo == null) {
            return "no existe un artículo con el código " + codigo;
        }
        if (articulo.getTipo() != TipoArticulo.PRODUCTO_TERMINADO) {
            return "el artículo " + codigo + " no es un producto terminado";
        }
        // 7.3 Cantidad mayor o igual a 1
        if (cantidad < 1) {
            return "la Cantidad debe ser mayor o igual a 1";
        }
        // 7.4 Existencias suficientes
        if (!articulo.alcanzaPara(BigDecimal.valueOf(cantidad))) {
            return "la cantidad solicitada (" + cantidad + ") supera las existencias ("
                    + articulo.getExistencias().stripTrailingZeros().toPlainString() + ") del artículo " + codigo;
        }
        // 7.5 Artículo repetido en la misma solicitud
        int primera = primeraLinea.get(codigo);
        if (primera < numero) {
            return "el artículo " + codigo + " ya está en la línea " + primera;
        }
        return null;
    }

    private static String codigo(Linea linea) {
        if (linea == null || linea.codigoArticulo() == null || linea.codigoArticulo().isBlank()) {
            return null;
        }
        return linea.codigoArticulo().trim();
    }
}
