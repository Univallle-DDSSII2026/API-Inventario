package co.edu.univalle.inventario.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.TipoArticulo;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Prueba de ArticuloService con ArticuloRepository contra un PostgreSQL real (Testcontainers):
 * consulta los artículos que carga la migración V2.
 */
@SpringBootTest
@ActiveProfiles("it")
class ArticuloServiceTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17.9-alpine"));

    static {
        POSTGRES.start();
    }

    @Autowired
    private ArticuloService articuloService;

    @Test
    void deberia_encontrar_un_articulo_que_existe() {
        // Act
        Optional<Articulo> articulo = articuloService.consultarArticulo("PT-00001");

        // Assert
        assertThat(articulo).isPresent();
        assertThat(articulo.get().getNombre()).isEqualTo("Cadena Ripit 5000");
        assertThat(articulo.get().getTipo()).isEqualTo(TipoArticulo.PRODUCTO_TERMINADO);
    }

    @Test
    void deberia_devolver_vacio_si_el_articulo_no_existe() {
        // Act & Assert
        assertThat(articuloService.consultarArticulo("PT-99999")).isEmpty();
    }
}