package co.edu.univalle.inventario.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

/**
 * Prueba el cliente contra un servidor HTTP local del JDK que hace las veces de la API de Notificaciones.
 */
class NotificacionesClientTest {

    private HttpServer servidor;
    private final AtomicInteger estadoRespuesta = new AtomicInteger(201);
    private final AtomicReference<String> apiKeyRecibida = new AtomicReference<>();
    private final AtomicReference<String> idRecibido = new AtomicReference<>();
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private NotificacionesClient cliente;

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/notificaciones", intercambio -> {
            apiKeyRecibida.set(intercambio.getRequestHeaders().getFirst("X-API-Key"));
            idRecibido.set(intercambio.getRequestHeaders().getFirst("X-Id-Notificacion"));
            cuerpoRecibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            intercambio.sendResponseHeaders(estadoRespuesta.get(), -1);
            intercambio.close();
        });
        servidor.start();
        cliente = new NotificacionesClient("http://localhost:" + servidor.getAddress().getPort(), "clave-de-prueba");
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    @Test
    void deberia_enviar_el_contenido_con_la_api_key_y_el_id_de_la_notificacion() {
        // Act
        cliente.enviar(77L, "{\"codigo\":\"PT-00001\"}");

        // Assert
        assertThat(apiKeyRecibida.get()).isEqualTo("clave-de-prueba");
        assertThat(idRecibido.get()).isEqualTo("77");
        assertThat(cuerpoRecibido.get()).isEqualTo("{\"codigo\":\"PT-00001\"}");
    }

    @Test
    void deberia_fallar_si_la_api_no_confirma_la_recepcion() {
        // Arrange
        estadoRespuesta.set(500);

        // Act & Assert
        assertThatThrownBy(() -> cliente.enviar(77L, "{}")).isInstanceOf(RestClientException.class);
    }
}
