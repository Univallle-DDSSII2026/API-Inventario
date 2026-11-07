package co.edu.univalle.inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import co.edu.univalle.inventario.client.NotificacionesClient;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Flujo transaccional de punta a punta contra un PostgreSQL real (Testcontainers):
 * HTTP → Controller → DTO → Service → Validator → Repository → PostgreSQL → respuesta → notificación.
 * La API de Notificaciones del Equipo 5 se reemplaza por un doble de Mockito.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class SalidaPedidoIT {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17.9-alpine"));

    static {
        POSTGRES.start();
    }

    @MockitoBean
    private NotificacionesClient notificacionesClient;

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${local.server.port}")
    private int puerto;

    private record Respuesta(int estado, String cuerpo) {
    }

    private Respuesta registrarSalida(String referencia, String codigo, int cantidad) {
        String hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String cuerpo = "{\"referenciaPedido\":\"" + referencia + "\",\"fecha\":\"" + hoy + "\",\"lineas\":["
                + "{\"codigoArticulo\":\"" + codigo + "\",\"cantidad\":" + cantidad + "},"
                + "{\"codigoArticulo\":\"PT-99999\",\"cantidad\":1}]}";
        return RestClient.create("http://localhost:" + puerto).post()
                .uri("/inventario/salidas/pedidos/productos-terminados")
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .exchange((peticion, respuesta) -> new Respuesta(respuesta.getStatusCode().value(),
                        new String(respuesta.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private void crearArticulo(String codigo, int existencias, int umbral) {
        jdbc.update("INSERT INTO articulo (codigo, nombre, tipo, existencias, umbral_minimo) "
                + "VALUES (?, ?, 'PRODUCTO_TERMINADO', ?, ?)", codigo, "Artículo " + codigo, existencias, umbral);
    }

    private BigDecimal existencias(String codigo) {
        return jdbc.queryForObject("SELECT existencias FROM articulo WHERE codigo = ?", BigDecimal.class, codigo);
    }

    /** Espera (hasta 10 s) a que el envío asíncrono intente la notificación y devuelve su estado. */
    private String estadoNotificacion(String codigo) throws InterruptedException {
        String sql = "SELECT n.estado FROM notificacion n JOIN articulo a ON a.id = n.articulo_id "
                + "WHERE a.codigo = ? AND n.fecha_ultimo_intento IS NOT NULL";
        Instant limite = Instant.now().plus(Duration.ofSeconds(10));
        List<String> estados = jdbc.queryForList(sql, String.class, codigo);
        while (estados.isEmpty() && Instant.now().isBefore(limite)) {
            Thread.sleep(100);
            estados = jdbc.queryForList(sql, String.class, codigo);
        }
        return estados.isEmpty() ? null : estados.get(0);
    }

    @Test
    void deberia_registrar_la_linea_valida_rechazar_la_invalida_y_enviar_la_notificacion_de_stock_bajo()
            throws Exception {
        // Arrange: 10 existencias y umbral 5; una salida de 6 deja el artículo en stock bajo.
        crearArticulo("PT-10001", 10, 5);

        // Act
        Respuesta respuesta = registrarSalida("PED-IT-1", "PT-10001", 6);

        // Assert
        assertThat(respuesta.estado()).isEqualTo(201);
        assertThat(respuesta.cuerpo())
                .contains("Salida registrada: 1 línea(s) registrada(s) y 1 línea(s) rechazada(s)")
                .contains("Línea 2: no existe un artículo con el código PT-99999");
        assertThat(existencias("PT-10001")).isEqualByComparingTo("4");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento WHERE referencia = 'PED-IT-1'",
                Integer.class)).isEqualTo(1);
        assertThat(estadoNotificacion("PT-10001")).isEqualTo("ENVIADA");
    }

    @Test
    void deberia_conservar_la_salida_aunque_falle_la_api_de_notificaciones() throws Exception {
        // Arrange
        crearArticulo("PT-10002", 10, 5);
        doThrow(new ResourceAccessException("API caída")).when(notificacionesClient).enviar(anyLong(), anyString());

        // Act
        Respuesta respuesta = registrarSalida("PED-IT-2", "PT-10002", 6);

        // Assert
        assertThat(respuesta.estado()).isEqualTo(201);
        assertThat(existencias("PT-10002")).isEqualByComparingTo("4");
        assertThat(estadoNotificacion("PT-10002")).isEqualTo("PENDIENTE");
    }

    @Test
    void deberia_responder_422_y_no_registrar_nada_si_todas_las_lineas_fallan() {
        // Arrange
        crearArticulo("PT-10003", 2, 0);

        // Act
        Respuesta respuesta = registrarSalida("PED-IT-3", "PT-10003", 5);

        // Assert
        assertThat(respuesta.estado()).isEqualTo(422);
        assertThat(respuesta.cuerpo()).contains("No se registró ninguna salida. Revise las líneas rechazadas");
        assertThat(existencias("PT-10003")).isEqualByComparingTo("2");
    }

    @Test
    void deberia_responder_400_si_falta_la_referencia() {
        // Act
        Respuesta respuesta = registrarSalida("", "PT-10001", 1);

        // Assert
        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.cuerpo()).contains("El campo Referencia del pedido es obligatorio");
    }

    @Test
    void deberia_aceptar_solo_las_salidas_que_alcanzan_cuando_llegan_al_mismo_tiempo() throws Exception {
        // Arrange: 10 existencias y 10 solicitudes simultáneas de 2 unidades; solo caben 5.
        crearArticulo("PT-20001", 10, 0);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(10);
        List<Future<Respuesta>> respuestas = new ArrayList<>();

        // Act
        for (int i = 0; i < 10; i++) {
            String referencia = "PED-CONC-" + i;
            respuestas.add(hilos.submit(() -> {
                salida.await();
                return registrarSalida(referencia, "PT-20001", 2);
            }));
        }
        salida.countDown();
        int aceptadas = 0;
        for (Future<Respuesta> respuesta : respuestas) {
            if (respuesta.get(30, TimeUnit.SECONDS).estado() == 201) {
                aceptadas++;
            }
        }
        hilos.shutdown();

        // Assert: nunca existencias negativas.
        assertThat(aceptadas).isEqualTo(5);
        assertThat(existencias("PT-20001")).isEqualByComparingTo("0");
    }
}
