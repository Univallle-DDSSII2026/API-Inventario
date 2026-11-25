package co.edu.univalle.inventario.repository;

import co.edu.univalle.inventario.model.Consecutivo;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * Repositorio de consecutivos (DAO, Data Access Layer).
 */
public interface ConsecutivoRepository extends JpaRepository<Consecutivo, String> {

    /**
     * Busca el consecutivo de un prefijo y bloquea su fila ({@code SELECT ... FOR UPDATE}): dos registros
     * simultáneos se atienden uno después del otro y nunca reciben el mismo código (DAS v1.3, Registro de artículos).
     * @param prefijo prefijo del código (por ejemplo, AT)
     * @return el consecutivo, o vacío si el prefijo no existe
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Consecutivo> findByPrefijo(String prefijo);
}