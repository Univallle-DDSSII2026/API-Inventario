package co.edu.univalle.inventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.univalle.inventario.command.RegistrarArticuloTercerosCommand;
import co.edu.univalle.inventario.exception.NombreDuplicadoException;
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
 * consulta los artículos que carga la migración V2 y registra artículos de terceros (HU_CUTIT_14).
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

    private static int numeroDe(Articulo articulo) {
        return Integer.parseInt(articulo.getCodigo().substring(3));
    }

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

    @Test
    void deberia_registrar_el_articulo_de_terceros_con_codigo_AT_y_existencias_y_umbral_en_cero() {
        // Act
        Articulo articulo = articuloService.registrar(
                new RegistrarArticuloTercerosCommand("Motosierra Stihl MS-250", "Con espada de 40 cm"));

        // Assert
        assertThat(articulo.getCodigo()).matches("AT-\\d{5}");
        assertThat(articulo.getNombre()).isEqualTo("Motosierra Stihl MS-250");
        assertThat(articulo.getDescripcion()).isEqualTo("Con espada de 40 cm");
        assertThat(articulo.getTipo()).isEqualTo(TipoArticulo.ARTICULO_TERCEROS);
        assertThat(articulo.getExistencias()).isEqualByComparingTo("0");
        assertThat(articulo.getUmbralMinimo()).isEqualByComparingTo("0");
        assertThat(articuloService.consultarArticulo(articulo.getCodigo())).isPresent();
    }

    @Test
    void deberia_generar_codigos_consecutivos() {
        // Act
        Articulo primero = articuloService.registrar(new RegistrarArticuloTercerosCommand("Kit consecutivo A", null));
        Articulo segundo = articuloService.registrar(new RegistrarArticuloTercerosCommand("Kit consecutivo B", null));

        // Assert
        assertThat(numeroDe(segundo)).isEqualTo(numeroDe(primero) + 1);
    }

    @Test
    void deberia_guardar_la_descripcion_vacia_como_nula() {
        // Act
        Articulo articulo = articuloService.registrar(new RegistrarArticuloTercerosCommand("Kit sin descripcion", ""));

        // Assert
        assertThat(articulo.getDescripcion()).isNull();
    }

    @Test
    void deberia_rechazar_el_nombre_repetido_sin_importar_las_mayusculas() {
        // Arrange
        articuloService.registrar(new RegistrarArticuloTercerosCommand("Kit Repetido", null));

        // Act & Assert
        assertThatThrownBy(() -> articuloService.registrar(new RegistrarArticuloTercerosCommand("KIT REPETIDO", null)))
                .isInstanceOf(NombreDuplicadoException.class)
                .hasMessage("Ya existe un artículo con el nombre KIT REPETIDO");
    }

    @Test
    void deberia_rechazar_el_nombre_de_un_articulo_de_otro_tipo() {
        // Act & Assert: "Cadena Ripit 5000" es un producto terminado de la migración V2.
        assertThatThrownBy(() -> articuloService.registrar(
                new RegistrarArticuloTercerosCommand("cadena ripit 5000", null)))
                .isInstanceOf(NombreDuplicadoException.class)
                .hasMessage("Ya existe un artículo con el nombre cadena ripit 5000");
    }

    @Test
    void no_deberia_gastar_un_codigo_cuando_el_registro_se_rechaza() {
        // Arrange
        Articulo antes = articuloService.registrar(new RegistrarArticuloTercerosCommand("Kit rollback A", null));

        // Act: el rechazo hace rollback, incluido el avance del consecutivo.
        assertThatThrownBy(() -> articuloService.registrar(
                new RegistrarArticuloTercerosCommand("kit rollback a", null)))
                .isInstanceOf(NombreDuplicadoException.class);

        Articulo despues = articuloService.registrar(new RegistrarArticuloTercerosCommand("Kit rollback B", null));

        // Assert
        assertThat(numeroDe(despues)).isEqualTo(numeroDe(antes) + 1);
    }
}