package co.edu.univalle.inventario.service;

import co.edu.univalle.inventario.command.RegistrarArticuloTercerosCommand;
import co.edu.univalle.inventario.exception.NombreDuplicadoException;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.Consecutivo;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import co.edu.univalle.inventario.repository.ConsecutivoRepository;
import co.edu.univalle.inventario.validator.ArticuloValidator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de artículos (Service de la Logic Layer; DAS v1.3, Figura 5).
 */
@Service
@RequiredArgsConstructor
public class ArticuloService {

    private static final String PREFIJO_ARTICULO_TERCEROS = "AT";

    private final ArticuloRepository articuloRepository;
    private final ConsecutivoRepository consecutivoRepository;
    private final ArticuloValidator articuloValidator;

    /**
     * Consulta un artículo por su código.
     * @param codigo código del artículo (por ejemplo, AT-00001)
     * @return el artículo, o vacío si no existe
     */
    @Transactional(readOnly = true)
    public Optional<Articulo> consultarArticulo(String codigo) {
        return articuloRepository.findByCodigo(codigo);
    }

    /**
     * Registra un artículo de terceros (HU_CUTIT_14): valida que el nombre sea único, bloquea el consecutivo del
     * prefijo AT, genera el código y guarda el artículo con Existencias 0 y Umbral mínimo 0.
     * @param command nombre y descripción ya ajustados
     * @return el artículo guardado, con su código
     * @throws NombreDuplicadoException si ya existe un artículo con ese nombre
     */
    @Transactional
    public Articulo registrar(RegistrarArticuloTercerosCommand command) {
        articuloValidator.validarNombre(command.nombre());

        Consecutivo consecutivo = consecutivoRepository.findByPrefijo(PREFIJO_ARTICULO_TERCEROS)
                .orElseThrow(() -> new IllegalStateException("No existe el consecutivo del prefijo AT"));
        String codigo = consecutivo.siguiente();

        Articulo articulo = Articulo.deTerceros(codigo, command.nombre(), descripcionOpcional(command.descripcion()));
        try {
            return articuloRepository.saveAndFlush(articulo);
        } catch (DataIntegrityViolationException excepcion) {
            // El índice único de nombre rechazó un registro simultáneo con el mismo nombre.
            throw new NombreDuplicadoException(command.nombre());
        }
    }

    private static String descripcionOpcional(String descripcion) {
        return descripcion == null || descripcion.isBlank() ? null : descripcion;
    }
}