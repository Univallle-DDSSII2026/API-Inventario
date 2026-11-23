package co.edu.univalle.inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.TipoArticulo;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * HU_CUTIT_06 de punta a punta contra un PostgreSQL real (Testcontainers), un caso por criterio de aceptación:
 * HTTP → Controller → DTO → Service → Validator → Repository → PostgreSQL → respuesta. Usa los artículos de la
 * migración V2 (PT-00001 con 10 existencias, PT-00002 con 3 y la materia prima MP-00001 en kg).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class DefinirUmbralIT {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17.9-alpine"));

    static {
        POSTGRES.start();
    }

    @Value("${local.server.port}")
    private int puerto;

    @Autowired
    private ArticuloRepository articuloRepository;

    private record Respuesta(int estado, String cuerpo) {
    }

    private Respuesta definirUmbral(String codigo, String cuerpoJson) {
        return RestClient.create("http://localhost:" + puerto).put()
                .uri("/inventario/articulos/{codigo}/umbral", codigo)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpoJson)
                .exchange((peticion, respuesta) -> new Respuesta(respuesta.getStatusCode().value(),
                        new String(respuesta.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private Respuesta consultar(String codigo) {
        return RestClient.create("http://localhost:" + puerto).get()
                .uri("/inventario/articulos/{codigo}", codigo)
                .exchange((peticion, respuesta) -> new Respuesta(respuesta.getStatusCode().value(),
                        new String(respuesta.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    @Test
    void deberia_responder_200_con_el_mensaje_del_criterio_5_y_reemplazar_el_umbral() {
        // Act
        Respuesta respuesta = definirUmbral("PT-00001", "{\"umbralMinimo\":8}");

        // Assert
        assertThat(respuesta.estado()).isEqualTo(200);
        assertThat(respuesta.cuerpo()).contains("Umbral mínimo de PT-00001 actualizado a 8");
        assertThat(consultar("PT-00001").cuerpo()).contains("\"umbralMinimo\":8");
    }

    @Test
    void deberia_aceptar_el_umbral_escrito_como_texto_y_con_espacios() {
        Respuesta respuesta = definirUmbral(" PT-00001 ", "{\"umbralMinimo\":\" 7 \"}");

        assertThat(respuesta.estado()).isEqualTo(200);
        assertThat(respuesta.cuerpo()).contains("Umbral mínimo de PT-00001 actualizado a 7");
    }

    @Test
    void deberia_responder_400_si_el_umbral_esta_vacio_o_no_viene() {
        assertThat(definirUmbral("PT-00001", "{}").cuerpo()).contains("El campo Umbral mínimo es obligatorio");
        assertThat(definirUmbral("PT-00001", "{\"umbralMinimo\":null}").cuerpo())
                .contains("El campo Umbral mínimo es obligatorio");
        Respuesta vacio = definirUmbral("PT-00001", "{\"umbralMinimo\":\"  \"}");

        assertThat(vacio.estado()).isEqualTo(400);
        assertThat(vacio.cuerpo()).contains("El campo Umbral mínimo es obligatorio");
    }

    @Test
    void deberia_responder_400_si_el_codigo_esta_vacio() {
        Respuesta respuesta = definirUmbral(" ", "{\"umbralMinimo\":5}");

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.cuerpo()).contains("El campo Código del artículo es obligatorio");
    }

    @Test
    void deberia_responder_400_si_el_codigo_no_tiene_el_formato() {
        Respuesta respuesta = definirUmbral("PT-1", "{\"umbralMinimo\":5}");

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.cuerpo()).contains("El Código del artículo no tiene el formato XX-00000");
    }

    @Test
    void deberia_responder_404_si_no_existe_un_articulo_con_ese_codigo() {
        Respuesta respuesta = definirUmbral("PT-99999", "{\"umbralMinimo\":5}");

        assertThat(respuesta.estado()).isEqualTo(404);
        assertThat(respuesta.cuerpo()).contains("No existe un artículo con el código PT-99999");
    }

    @Test
    void deberia_responder_400_si_el_umbral_no_es_numerico() {
        Respuesta respuesta = definirUmbral("PT-00001", "{\"umbralMinimo\":\"abc\"}");

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.cuerpo()).contains("El Umbral mínimo debe ser un valor numérico");
    }

    @Test
    void deberia_responder_400_si_el_umbral_es_menor_que_0() {
        Respuesta respuesta = definirUmbral("PT-00001", "{\"umbralMinimo\":-1}");

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.cuerpo()).contains("El Umbral mínimo debe ser mayor o igual a 0");
    }

    @Test
    void deberia_responder_400_si_un_producto_terminado_recibe_un_umbral_con_decimales() {
        Respuesta respuesta = definirUmbral("PT-00001", "{\"umbralMinimo\":2.5}");

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.cuerpo()).contains("El Umbral mínimo del artículo PT-00001 debe ser un número entero");
    }

    @Test
    void deberia_responder_400_si_una_materia_prima_en_und_o_un_articulo_de_terceros_recibe_decimales() {
        // Arrange
        articuloRepository.saveAndFlush(
                Articulo.nuevo(TipoArticulo.MATERIA_PRIMA, "MP-00050", "Tornillo IT", null, "und"));
        articuloRepository.saveAndFlush(
                Articulo.nuevo(TipoArticulo.ARTICULO_TERCEROS, "AT-00050", "Kit de repuestos IT", null));

        // Act & Assert
        assertThat(definirUmbral("MP-00050", "{\"umbralMinimo\":2.5}").cuerpo())
                .contains("El Umbral mínimo del artículo MP-00050 debe ser un número entero");
        assertThat(definirUmbral("AT-00050", "{\"umbralMinimo\":1.5}").cuerpo())
                .contains("El Umbral mínimo del artículo AT-00050 debe ser un número entero");
        assertThat(definirUmbral("MP-00050", "{\"umbralMinimo\":4}").estado()).isEqualTo(200);
    }

    @Test
    void deberia_aceptar_hasta_2_decimales_y_rechazar_mas_en_una_materia_prima_con_otra_unidad() {
        Respuesta aceptada = definirUmbral("MP-00001", "{\"umbralMinimo\":12.5}");
        Respuesta rechazada = definirUmbral("MP-00001", "{\"umbralMinimo\":12.555}");

        assertThat(aceptada.estado()).isEqualTo(200);
        assertThat(aceptada.cuerpo()).contains("Umbral mínimo de MP-00001 actualizado a 12.50");
        assertThat(rechazada.estado()).isEqualTo(400);
        assertThat(rechazada.cuerpo()).contains("El Umbral mínimo admite máximo 2 decimales");
    }

    @Test
    void deberia_aceptar_un_umbral_mayor_que_las_existencias_actuales() {
        // PT-00002 tiene 3 existencias; el criterio 7 no pone valor máximo.
        Respuesta respuesta = definirUmbral("PT-00002", "{\"umbralMinimo\":100000}");

        assertThat(respuesta.estado()).isEqualTo(200);
        assertThat(respuesta.cuerpo()).contains("Umbral mínimo de PT-00002 actualizado a 100000");
    }

    @Test
    void deberia_mostrar_stock_bajo_si_las_existencias_son_menores_y_normal_si_son_iguales_o_mayores() {
        // PT-00002 tiene 3 existencias.
        definirUmbral("PT-00002", "{\"umbralMinimo\":4}");
        assertThat(consultar("PT-00002").cuerpo()).contains("\"estadoExistencias\":\"Stock bajo\"");

        definirUmbral("PT-00002", "{\"umbralMinimo\":3}");
        assertThat(consultar("PT-00002").cuerpo()).contains("\"estadoExistencias\":\"Normal\"");

        definirUmbral("PT-00002", "{\"umbralMinimo\":2}");
        assertThat(consultar("PT-00002").cuerpo()).contains("\"estadoExistencias\":\"Normal\"");
    }
}