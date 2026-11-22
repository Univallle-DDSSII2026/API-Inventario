package co.edu.univalle.inventario.repository;

import co.edu.univalle.inventario.model.Articulo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de artículos (DAO, Data Access Layer).
 */
public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    /**
     * Busca un artículo por su código.
     * @param codigo código del artículo (por ejemplo, PT-00001)
     * @return el artículo, o vacío si no existe
     */
    Optional<Articulo> findByCodigo(String codigo);

    /**
     * Indica si ya existe un artículo de cualquier tipo con ese nombre, sin importar mayúsculas.
     * @param nombre nombre ya normalizado
     * @return true si el nombre ya está en uso
     */
    boolean existsByNombreIgnoreCase(String nombre);


}