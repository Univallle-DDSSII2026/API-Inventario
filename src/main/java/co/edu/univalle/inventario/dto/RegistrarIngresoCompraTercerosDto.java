package co.edu.univalle.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Cuerpo de {@code POST /inventario/ingresos/compras/articulos-terceros} (HU_CUTIT_13).
 * Conserva los valores JSON hasta que el validador los compruebe campo por campo.
 *
 * @param codigo código del artículo de terceros
 * @param cantidad unidades compradas como número entero mayor o igual a 1
 * @param fecha fecha de compra en formato DD/MM/AAAA
 * @param numeroFactura número de factura u orden de compra (1 a 30 caracteres)
 * @param proveedor nombre del proveedor (1 a 100 caracteres)
 * @param costoUnitario costo unitario en COP mayor que 0 y con máximo 2 decimales
 */
public record RegistrarIngresoCompraTercerosDto(
        @Schema(type = "string", example = "AT-00001") Object codigo,
        @Schema(type = "integer", example = "5")
        @JsonDeserialize(using = RegistrarIngresoFabricacionDto.CantidadDeserializer.class) String cantidad,
        @Schema(type = "string", example = "24/11/2026") Object fecha,
        @Schema(type = "string", maxLength = 30, example = "FAC-2026-001") Object numeroFactura,
        @Schema(type = "string", maxLength = 100, example = "Distribuidora Industrial S.A.") Object proveedor,
        @Schema(type = "number", example = "1250000.50")
        @JsonDeserialize(using = RegistrarIngresoFabricacionDto.CantidadDeserializer.class) String costoUnitario) {
}
