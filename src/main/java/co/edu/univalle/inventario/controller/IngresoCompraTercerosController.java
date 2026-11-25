package co.edu.univalle.inventario.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.univalle.inventario.command.RegistrarIngresoCompraTercerosCommand;
import co.edu.univalle.inventario.dto.IngresoCompraTercerosRegistradoDto;
import co.edu.univalle.inventario.dto.RegistrarIngresoCompraTercerosDto;
import co.edu.univalle.inventario.service.IngresoCompraTercerosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

/**
 * Endpoint de registro de ingresos por compra de artículos de terceros (HU_CUTIT_13).
 */
@RestController
@RequestMapping("/inventario/ingresos")
@RequiredArgsConstructor
public class IngresoCompraTercerosController {

    private final IngresoCompraTercerosService servicio;

    /**
     * Registra un ingreso por compra de artículos de terceros.
     *
     * @param dto datos de la compra
     * @return confirmación con el saldo actualizado
     */
    @PostMapping("/compras/articulos-terceros")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar ingreso por compra de artículos de terceros")
    @ApiResponse(responseCode = "201", description = "Ingreso registrado")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @ApiResponse(responseCode = "404", description = "Artículo inexistente")
    public IngresoCompraTercerosRegistradoDto registrar(
            @RequestBody(required = false) RegistrarIngresoCompraTercerosDto dto) {
        RegistrarIngresoCompraTercerosCommand comando = dto == null
                ? new RegistrarIngresoCompraTercerosCommand(null, null, null, null, null, null)
                : new RegistrarIngresoCompraTercerosCommand(dto.codigo(), dto.cantidad(), dto.fecha(),
                        dto.numeroFactura(), dto.proveedor(), dto.costoUnitario());
        return new IngresoCompraTercerosRegistradoDto(servicio.registrar(comando));
    }
}
