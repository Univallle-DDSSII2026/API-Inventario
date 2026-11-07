package co.edu.univalle.inventario.repository;

import co.edu.univalle.inventario.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de notificaciones (DAO, Data Access Layer).
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
}
