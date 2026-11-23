package co.edu.univalle.inventario.controller;

import co.edu.univalle.inventario.command.RegistrarArticuloTercerosCommand;
import co.edu.univalle.inventario.dto.ArticuloDto;
import co.edu.univalle.inventario.dto.RegistrarArticuloTercerosDto;
import co.edu.univalle.inventario.dto.RegistroArticuloDto;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.service.ArticuloService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de MVC (View Layer): recibe la petición y delega en el Service.
 */
@RestController
@RequestMapping("/inventario")
@RequiredArgsConstructor
public class ArticuloController {

    private final ArticuloService articuloService;

    /**
     * Consulta un artículo por su código.
     * @param codigo código del artículo (por ejemplo, PT-00001)
     * @return 200 con el artículo, o 404 si no existe
     */
    @GetMapping("/articulos/{codigo}")
    @Operation(summary = "Consultar un artículo por su código")
    public ResponseEntity<ArticuloDto> consultarArticulo(@PathVariable("codigo") String codigo) {
        return articuloService.consultarArticulo(codigo)
                .map(ArticuloDto::de)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Registra un artículo de terceros (HU_CUTIT_14).
     * @param dto nombre y descripción del artículo
     * @return 201 con el mensaje de confirmación y el artículo creado
     */
    @PostMapping("/articulos-terceros")
    @Operation(summary = "Registrar un artículo de terceros")
    public ResponseEntity<RegistroArticuloDto> registrar(@Valid @RequestBody RegistrarArticuloTercerosDto dto) {
        Articulo articulo = articuloService.registrar(
                new RegistrarArticuloTercerosCommand(dto.nombre(), dto.descripcion()));
        return ResponseEntity.status(HttpStatus.CREATED).body(RegistroArticuloDto.de(articulo));
    }


    /**
     * Registra un producto terminado, cadena o cuchilla (HU_CUTIT_01). Usa el mismo cuerpo y las mismas validaciones
     * que el registro de artículos de terceros.
     * @param dto nombre y descripción del producto
     * @return 201 con el mensaje de confirmación y el artículo creado
     */
    @PostMapping("/productos-terminados")
    @Operation(summary = "Registrar un producto terminado")
    public ResponseEntity<RegistroArticuloDto> registrarProductoTerminado(
            @Valid @RequestBody RegistrarArticuloTercerosDto dto) {
        Articulo articulo = articuloService.registrarProductoTerminado(
                new RegistrarArticuloTercerosCommand(dto.nombre(), dto.descripcion()));
        return ResponseEntity.status(HttpStatus.CREATED).body(RegistroArticuloDto.de(articulo));
    }
}