package co.edu.univalle.inventario.controller;

import co.edu.univalle.inventario.dto.SalidaPedidoRequest;
import co.edu.univalle.inventario.dto.SalidaResponse;
import co.edu.univalle.inventario.service.MovimientoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de MVC (View Layer): recibe la petición, valida el DTO y delega en el Service.
 */
@RestController
@RequestMapping("/inventario/salidas/pedidos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService movimientoService;

    /**
     * Registra la salida de productos terminados por pedido (HU_04).
     * @param solicitud referencia del pedido, fecha y líneas a descontar
     * @return 201 con las líneas registradas y las rechazadas
     */
    @PostMapping("/productos-terminados")
    @PreAuthorize("hasRole('EQUIPO')")
    @Operation(summary = "Registrar la salida de productos terminados entregados en un pedido (HU_CUTIT_04)")
    public ResponseEntity<SalidaResponse> registrarSalida(@Valid @RequestBody SalidaPedidoRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movimientoService.registrarSalida(solicitud));
    }
}
