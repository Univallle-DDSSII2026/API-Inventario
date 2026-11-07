package co.edu.univalle.inventario.repository;

import co.edu.univalle.inventario.model.Articulo;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de artículos (DAO, Data Access Layer).
 */
public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    /**
     * {@code SELECT ... FOR UPDATE} en orden de código: dos salidas simultáneas del mismo artículo se atienden una
     * después de la otra y nunca dejan existencias negativas (DAS v1.3, Tabla 7 — Integridad ante concurrencia).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Articulo a where a.codigo in :codigos order by a.codigo")
    List<Articulo> bloquearPorCodigos(@Param("codigos") Collection<String> codigos);
}
