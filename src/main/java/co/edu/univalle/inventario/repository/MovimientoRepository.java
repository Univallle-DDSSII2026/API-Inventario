package co.edu.univalle.inventario.repository;

import co.edu.univalle.inventario.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de movimientos (DAO, Data Access Layer).
 */
public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {
}
