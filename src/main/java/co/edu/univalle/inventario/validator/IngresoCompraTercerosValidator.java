package co.edu.univalle.inventario.validator;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import co.edu.univalle.inventario.exception.SolicitudInvalidaException;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.TipoArticulo;
import lombok.RequiredArgsConstructor;

/**
 * Valida los campos y reglas de negocio del ingreso por compra de artículos de terceros (HU_CUTIT_13).
 */
@Component
@RequiredArgsConstructor
public class IngresoCompraTercerosValidator {

    private static final int MAX_LONGITUD_FACTURA = 30;
    private static final int MAX_LONGITUD_PROVEEDOR = 100;
    private static final int MAX_DECIMALES_COSTO = 2;

    private final IngresoFabricacionValidator ingresoFabricacionValidator;

    /**
     * Comprueba que los seis campos obligatorios del ingreso estén presentes y no vacíos (criterio 2).
     *
     * @param codigo código del artículo recibido
     * @param cantidad cantidad recibida como texto
     * @param fecha fecha recibida
     * @param numeroFactura número de factura u orden de compra recibido
     * @param proveedor proveedor recibido
     * @param costoUnitario costo unitario recibido como texto
     */
    public void validarObligatorios(Object codigo, String cantidad, Object fecha,
            Object numeroFactura, Object proveedor, String costoUnitario) {
        exigirCampo(codigo, "Código del artículo");
        exigirCampo(cantidad, "Cantidad");
        exigirCampo(fecha, "Fecha");
        exigirCampo(numeroFactura, "Número de factura u orden de compra");
        exigirCampo(proveedor, "Proveedor");
        exigirCampo(costoUnitario, "Costo unitario");
    }

    /**
     * Valida el código obligatorio y su formato XX-00000 (criterios 2 y 3).
     *
     * @param valor código recibido
     * @return código normalizado sin espacios en los extremos
     */
    public String validarCodigo(Object valor) {
        return ingresoFabricacionValidator.validarCodigo(valor);
    }

    /**
     * Exige que el artículo existente sea un artículo de terceros (criterio 3).
     *
     * @param articulo artículo consultado en base de datos
     */
    public void validarTipo(Articulo articulo) {
        if (articulo.getTipo() != TipoArticulo.ARTICULO_TERCEROS) {
            throw new SolicitudInvalidaException(
                    "El artículo %s no es un artículo de terceros".formatted(articulo.getCodigo()));
        }
    }

    /**
     * Convierte la cantidad a un número entero mayor o igual a 1 (criterio 4).
     *
     * @param valor cantidad recibida como texto
     * @return cantidad validada de precisión exacta
     */
    public BigDecimal validarCantidad(String valor) {
        return ingresoFabricacionValidator.validarCantidad(valor);
    }

    /**
     * Valida una fecha real en formato DD/MM/AAAA dentro de los últimos 30 días (criterio 5).
     *
     * @param valor fecha recibida
     * @param hoy fecha actual en la zona horaria del inventario
     * @return fecha validada
     */
    public LocalDate validarFecha(Object valor, LocalDate hoy) {
        return ingresoFabricacionValidator.validarFecha(valor, hoy);
    }

    /**
     * Valida el número de factura u orden de compra de 1 a 30 caracteres (criterios 2 y 6).
     *
     * @param valor número de factura u orden de compra recibido
     * @return texto validado sin alteraciones
     */
    public String validarNumeroFactura(Object valor) {
        return validarTexto(valor, "Número de factura u orden de compra", MAX_LONGITUD_FACTURA);
    }

    /**
     * Valida el proveedor de 1 a 100 caracteres (criterios 2 y 6).
     *
     * @param valor proveedor recibido
     * @return texto validado sin alteraciones
     */
    public String validarProveedor(Object valor) {
        return validarTexto(valor, "Proveedor", MAX_LONGITUD_PROVEEDOR);
    }

    /**
     * Valida el costo unitario en COP: numérico, mayor que 0 y con máximo 2 decimales (criterios 2 y 7).
     *
     * @param valor costo unitario recibido como texto
     * @return costo unitario validado
     */
    public BigDecimal validarCostoUnitario(String valor) {
        exigirCampo(valor, "Costo unitario");
        BigDecimal costo;
        try {
            costo = new BigDecimal(valor.strip());
        } catch (NumberFormatException excepcion) {
            throw new SolicitudInvalidaException("El Costo unitario debe ser un valor numérico");
        }
        if (costo.signum() <= 0) {
            throw new SolicitudInvalidaException("El Costo unitario debe ser mayor que 0");
        }
        if (costo.scale() > 0) {
            costo = costo.stripTrailingZeros();
        }
        if (costo.scale() > MAX_DECIMALES_COSTO) {
            throw new SolicitudInvalidaException("El Costo unitario admite máximo 2 decimales");
        }
        return costo;
    }

    /**
     * Comprueba que el valor numérico sea representable de forma exacta por PostgreSQL.
     *
     * @param valor número decimal ya validado
     */
    public void validarRepresentabilidad(BigDecimal valor) {
        ingresoFabricacionValidator.validarRepresentabilidad(valor);
    }

    private String validarTexto(Object valor, String campo, int longitudMaxima) {
        exigirCampo(valor, campo);
        if (!(valor instanceof String texto)) {
            throw new SolicitudInvalidaException("El campo %s debe ser texto".formatted(campo));
        }
        if (texto.codePointCount(0, texto.length()) > longitudMaxima) {
            throw new SolicitudInvalidaException(
                    "El campo %s no puede superar %d caracteres".formatted(campo, longitudMaxima));
        }
        return texto;
    }

    private void exigirCampo(Object valor, String campo) {
        if (valor == null || valor instanceof String texto && texto.isBlank()) {
            throw new SolicitudInvalidaException("El campo %s es obligatorio".formatted(campo));
        }
    }
}
