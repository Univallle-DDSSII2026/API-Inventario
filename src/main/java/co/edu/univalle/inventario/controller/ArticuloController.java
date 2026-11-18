package co.edu.univalle.inventario.controller;

import co.edu.univalle.inventario.dto.ArticuloDto;
import co.edu.univalle.inventario.service.ArticuloService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de MVC (View Layer): recibe la petición y delega en el Service.
 */
@RestController
@RequestMapping("/inventario/articulos")
@RequiredArgsConstructor
public class ArticuloController {

    private final ArticuloService articuloService;

    /**
     * Consulta un artículo por su código.
     * @param codigo código del artículo (por ejemplo, PT-00001)
     * @return 200 con el artículo, o 404 si no existe
     */
    @GetMapping("/{codigo}")
    @Operation(summary = "Consultar un artículo por su código")
    public ResponseEntity<ArticuloDto> consultarArticulo(@PathVariable("codigo") String codigo) {
        return articuloService.consultarArticulo(codigo)
                .map(ArticuloDto::de)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}