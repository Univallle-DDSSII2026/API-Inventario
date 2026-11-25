package co.edu.univalle.inventario.command;

/**
 * Datos de entrada para registrar un ingreso por compra de artículos de terceros (HU_CUTIT_13).
 *
 * @param codigo código del artículo recibido
 * @param cantidad cantidad comprada recibida como texto
 * @param fecha fecha de compra recibida
 * @param numeroFactura número de factura u orden de compra recibido
 * @param proveedor nombre del proveedor recibido
 * @param costoUnitario costo unitario en COP recibido como texto
 */
public record RegistrarIngresoCompraTercerosCommand(
        Object codigo,
        String cantidad,
        Object fecha,
        Object numeroFactura,
        Object proveedor,
        String costoUnitario) {
}
