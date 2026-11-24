package co.edu.univalle.inventario.controller;

import co.edu.univalle.inventario.command.RegistrarIngresoFabricacionCommand;
import co.edu.univalle.inventario.dto.IngresoFabricacionRegistradoDto;
import co.edu.univalle.inventario.dto.RegistrarIngresoFabricacionDto;
import co.edu.univalle.inventario.service.IngresoFabricacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint de ingreso de unidades fabricadas. */
@RestController
@RequestMapping("/inventario/ingresos")
@RequiredArgsConstructor
public class IngresoFabricacionController {

    private final IngresoFabricacionService servicio;

    /**
     * Registra un ingreso de fabricación.
     * @param dto datos del ingreso
     * @return confirmación con el saldo actualizado
     */
    @PostMapping("/fabricacion")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar ingreso de unidades fabricadas")
    @ApiResponse(responseCode = "201", description = "Ingreso registrado")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @ApiResponse(responseCode = "404", description = "Artículo inexistente")
    public IngresoFabricacionRegistradoDto registrar(
            @RequestBody(required = false) RegistrarIngresoFabricacionDto dto) {
        RegistrarIngresoFabricacionCommand comando = dto == null
                ? new RegistrarIngresoFabricacionCommand(null, null, null, null)
                : new RegistrarIngresoFabricacionCommand(
                        dto.codigo(), dto.cantidad(), dto.fecha(), dto.referenciaFabricacion());
        return new IngresoFabricacionRegistradoDto(servicio.registrar(comando));
    }
}
