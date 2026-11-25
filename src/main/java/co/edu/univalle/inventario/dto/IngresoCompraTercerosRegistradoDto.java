package co.edu.univalle.inventario.dto;

/**
 * Confirmación del ingreso por compra de artículos de terceros (HU_CUTIT_13, criterio 8).
 *
 * @param mensaje mensaje de confirmación con la cantidad, el artículo y las existencias actuales
 */
public record IngresoCompraTercerosRegistradoDto(String mensaje) {
}
