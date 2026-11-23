package co.edu.univalle.inventario.controller;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.univalle.inventario.dto.ArticuloDto;
import co.edu.univalle.inventario.dto.RegistrarArticuloTercerosDto;
import co.edu.univalle.inventario.dto.RegistroArticuloDto;
import co.edu.univalle.inventario.model.TipoArticulo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Prueba de ArticuloController con el Service y el Repository reales contra PostgreSQL (Testcontainers).
 */
@SpringBootTest
@ActiveProfiles("it")
class ArticuloControllerTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17.9-alpine"));

    static {
        POSTGRES.start();
    }

    @Autowired
    private ArticuloController articuloController;

    @Test
    void deberia_responder_200_con_el_articulo_si_existe() {
        // Act
        ResponseEntity<ArticuloDto> respuesta = articuloController.consultarArticulo("PT-00001");

        // Assert
        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().codigo()).isEqualTo("PT-00001");
    }

    @Test
    void deberia_responder_404_si_el_articulo_no_existe() {
        // Act & Assert
        assertThat(articuloController.consultarArticulo("PT-99999").getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void deberia_responder_201_con_el_mensaje_de_confirmacion_al_registrar_un_articulo_de_terceros() {
        // Act
        ResponseEntity<RegistroArticuloDto> respuesta =
                articuloController.registrar(new RegistrarArticuloTercerosDto("Kit de cadena Ripit", null));

        // Assert
        assertThat(respuesta.getStatusCode().value()).isEqualTo(201);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().mensaje())
                .startsWith("Se registró el artículo AT-")
                .endsWith(" – Kit de cadena Ripit con Tipo Artículo de terceros");
    }


    @Test
    void deberia_responder_201_con_el_mensaje_de_confirmacion_al_registrar_un_producto_terminado() {
        // Act
        ResponseEntity<RegistroArticuloDto> respuesta = articuloController.registrarProductoTerminado(
                new RegistrarArticuloTercerosDto("Cuchilla Ripit 7000", null));

        // Assert
        assertThat(respuesta.getStatusCode().value()).isEqualTo(201);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().mensaje())
                .startsWith("Se registró el artículo PT-")
                .endsWith(" – Cuchilla Ripit 7000 con Tipo Producto terminado");
        assertThat(respuesta.getBody().articulo().tipo()).isEqualTo(TipoArticulo.PRODUCTO_TERMINADO);
    }
}