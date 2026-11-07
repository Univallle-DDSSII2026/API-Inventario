package co.edu.univalle.inventario.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente de la API de Notificaciones del Equipo 5 (Integration Layer; DAS v1.3, Tabla 10).
 * El formato del contenido es provisional hasta que lo confirme el Equipo 5.
 */
@Component
public class NotificacionesClient {

    private final RestClient restClient;
    private final String apiKey;

    /**
     * Crea el cliente del servicio de notificaciones con tiempos de espera acotados.
     * @param url URL base del servicio (variable NOTIFICACIONES_URL)
     * @param apiKey clave de acceso (variable NOTIFICACIONES_API_KEY)
     */
    public NotificacionesClient(@Value("${inventario.notificaciones.url}") String url,
                                @Value("${inventario.notificaciones.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(2));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
        this.apiKey = apiKey;
    }

    /**
     * Envía la notificación ({@code POST /notificaciones}).
     *
     * @throws org.springframework.web.client.RestClientException si la API no confirma la recepción
     */
    public void enviar(Long idNotificacion, String contenidoJson) {
        restClient.post()
                .uri("/notificaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", apiKey)
                .header("X-Id-Notificacion", String.valueOf(idNotificacion))
                .body(contenidoJson)
                .retrieve()
                .toBodilessEntity();
    }
}
