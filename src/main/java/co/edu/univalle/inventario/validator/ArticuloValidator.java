package co.edu.univalle.inventario.validator;

import co.edu.univalle.inventario.exception.NombreDuplicadoException;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validator de la Logic Layer: comprueba las reglas de negocio que dependen de los datos guardados (DAS v1.3,
 * Figura 5).
 */
@Component
@RequiredArgsConstructor
public class ArticuloValidator {

    private final ArticuloRepository articuloRepository;

    /**
     * Verifica que ningún artículo, de cualquier tipo, tenga ya ese nombre (sin importar mayúsculas).
     *
     * @param nombre nombre ya ajustado
     * @throws NombreDuplicadoException si el nombre ya está en uso
     */
    public void validarNombre(String nombre) {
        if (articuloRepository.existsByNombreIgnoreCase(nombre)) {
            throw new NombreDuplicadoException(nombre);
        }
    }
}