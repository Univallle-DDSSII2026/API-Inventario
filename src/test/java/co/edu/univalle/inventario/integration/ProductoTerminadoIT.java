package co.edu.univalle.inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * HU_CUTIT_01 de punta a punta contra un PostgreSQL real (Testcontainers), un caso por criterio de aceptación:
 * HTTP → Controller → DTO → Service → Validator → Repository → PostgreSQL → respuesta.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class ProductoTerminadoIT {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17.9-alpine"));

    static {
        POSTGRES.start();
    }

    @Value("${local.server.port}")
    private int puerto;

    private record Respuesta(int estado, String cuerpo) {
    }

    private Respuesta registrar(String cuerpoJson) {
        return RestClient.create("http://localhost:" + puerto).post()
                .uri("/inventario/productos-terminados")
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpoJson)
                .exchange((peticion, respuesta) -> new Respuesta(respuesta.getStatusCode().value(),
                        new String(respuesta.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    @Test
    void deberia_responder_201_con_el_mensaje_del_criterio_7_y_ajustar_los_espacios() {
        // Act
        Respuesta respuesta = registrar("{\"nombre\":\"  Cadena   Ripit  6000 \",\"descripcion\":\"Paso 3/8\"}");

        // Assert: el código sigue el consecutivo PT, que ya trae los productos de la migración V2.
        assertThat(respuesta.estado()).isEqualTo(201);
        String mensaje = "Se registró el artículo PT-\\d{5} – Cadena Ripit 6000 "
                + "con Tipo Producto terminado";
        assertThat(respuesta.cuerpo()).containsPattern(mensaje).contains("PRODUCTO_TERMINADO");
    }

    @Test
    void deberia_responder_400_si_el_nombre_esta_vacio() {
        Respuesta respuesta = registrar("{\"nombre\":\"   \"}");

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.cuerpo()).contains("El campo Nombre es obligatorio");
    }

    @Test
    void deberia_responder_400_si_el_nombre_es_muy_corto() {
        Respuesta respuesta = registrar("{\"nombre\":\"ab\"}");

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.cuerpo()).contains("El Nombre debe tener entre 3 y 100 caracteres");
    }

    @Test
    void deberia_responder_400_si_el_nombre_tiene_caracteres_no_permitidos() {
        Respuesta respuesta = registrar("{\"nombre\":\"Cuchilla @\"}");

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.cuerpo()).contains("El Nombre contiene caracteres no permitidos");
    }

    @Test
    void deberia_responder_400_si_la_descripcion_supera_500_caracteres() {
        Respuesta respuesta = registrar("{\"nombre\":\"Cuchilla\",\"descripcion\":\"" + "a".repeat(501) + "\"}");

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.cuerpo()).contains("La Descripción no puede superar 500 caracteres");
    }

    @Test
    void deberia_responder_409_si_el_nombre_ya_existe_sin_importar_las_mayusculas() {
        // Arrange
        registrar("{\"nombre\":\"Producto Duplicado IT\"}");

        // Act
        Respuesta respuesta = registrar("{\"nombre\":\"  PRODUCTO   duplicado it \"}");

        // Assert
        assertThat(respuesta.estado()).isEqualTo(409);
        assertThat(respuesta.cuerpo()).contains("Ya existe un artículo con el nombre PRODUCTO duplicado it");
    }

    @Test
    void deberia_responder_409_si_el_nombre_pertenece_a_un_articulo_de_otro_tipo() {
        // Act: "Acero templado" es una materia prima de la migración V2.
        Respuesta respuesta = registrar("{\"nombre\":\"ACERO templado\"}");

        // Assert
        assertThat(respuesta.estado()).isEqualTo(409);
        assertThat(respuesta.cuerpo()).contains("Ya existe un artículo con el nombre ACERO templado");
    }
}