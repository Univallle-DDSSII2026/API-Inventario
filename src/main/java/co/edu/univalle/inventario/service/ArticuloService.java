package co.edu.univalle.inventario.service;

import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de artículos (Service de la Logic Layer; DAS v1.3, Figura 5).
 */
@Service
@RequiredArgsConstructor
public class ArticuloService {

    private final ArticuloRepository articuloRepository;

    /**
     * Consulta un artículo por su código.
     * @param codigo código del artículo (por ejemplo, PT-00001)
     * @return el artículo, o vacío si no existe
     */
    @Transactional(readOnly = true)
    public Optional<Articulo> consultarArticulo(String codigo) {
        return articuloRepository.findByCodigo(codigo);
    }
}