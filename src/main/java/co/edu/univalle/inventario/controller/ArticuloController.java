package co.edu.univalle.inventario.controller;

import co.edu.univalle.inventario.command.RegistrarArticuloTercerosCommand;
import co.edu.univalle.inventario.command.RegistrarMateriaPrimaCommand;
import co.edu.univalle.inventario.dto.ArticuloDto;
import co.edu.univalle.inventario.dto.RegistrarArticuloTercerosDto;
import co.edu.univalle.inventario.dto.RegistroArticuloDto;
import co.edu.univalle.inventario.dto.RegistrarMateriaPrimaDto;
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
import co.edu.univalle.inventario.command.DefinirUmbralCommand;
import co.edu.univalle.inventario.dto.DefinirUmbralDto;
import co.edu.univalle.inventario.dto.UmbralActualizadoDto;
import org.springframework.web.bind.annotation.PutMapping;

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

    
    /**
     * Define el Umbral mínimo de un artículo (HU_CUTIT_06). Lo consume el sistema de Fabricación.
     * @param codigo código del artículo (por ejemplo, PT-00001)
     * @param dto cuerpo con el Umbral mínimo; si no se envía cuerpo se responde que el campo es obligatorio
     * @return 200 con el mensaje de confirmación y el artículo actualizado
     */
    @PutMapping("/articulos/{codigo}/umbral")
    @Operation(summary = "Definir el umbral mínimo de un artículo")
    public ResponseEntity<UmbralActualizadoDto> definirUmbral(@PathVariable("codigo") String codigo,
            @RequestBody(required = false) DefinirUmbralDto dto) {
        String umbralMinimo = dto == null ? null : dto.umbralMinimo();
        Articulo articulo = articuloService.definirUmbral(new DefinirUmbralCommand(codigo, umbralMinimo));
        return ResponseEntity.ok(UmbralActualizadoDto.de(articulo));
    }

        /**
     * Registra una materia prima (HU_CUTIT_11).
     * @param dto nombre, descripción y unidad de medida de la materia prima
     * @return 201 con el mensaje de confirmación y el artículo creado
     */
    @PostMapping("/materias-primas")
    @Operation(summary = "Registrar una materia prima")
    public ResponseEntity<RegistroArticuloDto> registrarMateriaPrima(
            @Valid @RequestBody RegistrarMateriaPrimaDto dto) {
        Articulo articulo = articuloService.registrarMateriaPrima(
                new RegistrarMateriaPrimaCommand(dto.nombre(), dto.descripcion(), dto.unidadMedida()));
        return ResponseEntity.status(HttpStatus.CREATED).body(RegistroArticuloDto.de(articulo));
    }
}