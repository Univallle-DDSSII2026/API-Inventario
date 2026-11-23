package co.edu.univalle.inventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.univalle.inventario.command.RegistrarArticuloTercerosCommand;
import co.edu.univalle.inventario.exception.NombreDuplicadoException;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.TipoArticulo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Prueba del registro de productos terminados (HU_CUTIT_01) con ArticuloService y ArticuloRepository contra un
 * PostgreSQL real (Testcontainers). Cada prueba usa un nombre propio: el índice único no deja repetir nombres.
 */
@SpringBootTest
@ActiveProfiles("it")
class ProductoTerminadoServiceTest {

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

    private Articulo registrar(String nombre, String descripcion) {
        return articuloService.registrarProductoTerminado(new RegistrarArticuloTercerosCommand(nombre, descripcion));
    }

    @Test
    void deberia_registrar_el_producto_terminado_con_codigo_PT_y_existencias_y_umbral_en_cero() {
        // Act
        Articulo articulo = registrar("Cadena Ripit 6000", "Paso 3/8, 62 eslabones");

        // Assert
        assertThat(articulo.getCodigo()).matches("PT-\\d{5}");
        assertThat(articulo.getNombre()).isEqualTo("Cadena Ripit 6000");
        assertThat(articulo.getDescripcion()).isEqualTo("Paso 3/8, 62 eslabones");
        assertThat(articulo.getTipo()).isEqualTo(TipoArticulo.PRODUCTO_TERMINADO);
        assertThat(articulo.getExistencias()).isEqualByComparingTo("0");
        assertThat(articulo.getUmbralMinimo()).isEqualByComparingTo("0");
        assertThat(articuloService.consultarArticulo(articulo.getCodigo())).isPresent();
    }

    @Test
    void deberia_continuar_el_consecutivo_PT_despues_de_los_productos_de_demostracion() {
        // Act
        Articulo articulo = registrar("Cuchilla Ripit 6000", null);

        // Assert: PT-00001 y PT-00002 los carga la migración V2.
        assertThat(numeroDe(articulo)).isGreaterThan(2);
    }

    @Test
    void deberia_generar_codigos_PT_consecutivos() {
        // Act
        Articulo primero = registrar("Producto consecutivo A", null);
        Articulo segundo = registrar("Producto consecutivo B", null);

        // Assert
        assertThat(numeroDe(segundo)).isEqualTo(numeroDe(primero) + 1);
    }

    @Test
    void deberia_llevar_un_consecutivo_PT_aparte_del_de_los_articulos_de_terceros() {
        // Arrange
        Articulo antes = registrar("Producto aparte A", null);

        // Act
        Articulo tercero = articuloService.registrar(new RegistrarArticuloTercerosCommand("Kit aparte", null));
        Articulo despues = registrar("Producto aparte B", null);

        // Assert
        assertThat(tercero.getCodigo()).startsWith("AT-");
        assertThat(numeroDe(despues)).isEqualTo(numeroDe(antes) + 1);
    }

    @Test
    void deberia_guardar_la_descripcion_vacia_como_nula() {
        // Act
        Articulo articulo = registrar("Producto sin descripcion", "");

        // Assert
        assertThat(articulo.getDescripcion()).isNull();
    }

    @Test
    void deberia_rechazar_el_nombre_repetido_sin_importar_las_mayusculas() {
        // Arrange
        registrar("Producto Repetido", null);

        // Act & Assert
        assertThatThrownBy(() -> registrar("PRODUCTO REPETIDO", null))
                .isInstanceOf(NombreDuplicadoException.class)
                .hasMessage("Ya existe un artículo con el nombre PRODUCTO REPETIDO");
    }

    @Test
    void deberia_rechazar_el_nombre_de_un_articulo_de_otro_tipo() {
        // Act & Assert: "Acero templado" es una materia prima de la migración V2.
        assertThatThrownBy(() -> registrar("acero TEMPLADO", null))
                .isInstanceOf(NombreDuplicadoException.class)
                .hasMessage("Ya existe un artículo con el nombre acero TEMPLADO");
    }

    @Test
    void deberia_rechazar_el_nombre_de_un_articulo_de_terceros() {
        // Arrange
        articuloService.registrar(new RegistrarArticuloTercerosCommand("Motosierra cruzada", null));

        // Act & Assert
        assertThatThrownBy(() -> registrar("MOTOSIERRA CRUZADA", null))
                .isInstanceOf(NombreDuplicadoException.class)
                .hasMessage("Ya existe un artículo con el nombre MOTOSIERRA CRUZADA");
    }

    @Test
    void no_deberia_gastar_un_codigo_cuando_el_registro_se_rechaza() {
        // Arrange
        Articulo antes = registrar("Producto rollback A", null);

        // Act: el rechazo hace rollback, incluido el avance del consecutivo.
        assertThatThrownBy(() -> registrar("producto rollback a", null))
                .isInstanceOf(NombreDuplicadoException.class);

        Articulo despues = registrar("Producto rollback B", null);

        // Assert
        assertThat(numeroDe(despues)).isEqualTo(numeroDe(antes) + 1);
    }
}