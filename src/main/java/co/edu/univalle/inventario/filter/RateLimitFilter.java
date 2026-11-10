package co.edu.univalle.inventario.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Primer filtro de cada petición: limita las peticiones por minuto de cada cliente y responde 429 al superarlo
 * (DAS v1.3, Tabla 5, Capacidad). El cliente se identifica por un resumen de su API Key y por su IP; el filtro corre
 * antes de validar la clave, así que una clave falsa no abre un límite nuevo para otra IP.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    /** Mensaje del 429 (Tabla 11). */
    public static final String MENSAJE_LIMITE = "Se superó el límite de peticiones. Espere un momento antes de "
            + "reintentar.";

    private final long limitePorMinuto;
    private final RespuestaError respuestaError;
    private final Cache<String, Bucket> contadores = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();

    /**
     * Crea el filtro.
     * @param limitePorMinuto máximo de peticiones por minuto de cada cliente (RATE_LIMIT_PER_MINUTE)
     * @param respuestaError escritor del cuerpo de error
     */
    public RateLimitFilter(long limitePorMinuto, RespuestaError respuestaError) {
        this.limitePorMinuto = limitePorMinuto;
        this.respuestaError = respuestaError;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        // El health check de Docker consulta seguido; no cuenta contra el límite.
        return peticion.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        Bucket contador = contadores.get(cliente(peticion), clave -> nuevoContador());
        ConsumptionProbe intento = contador.tryConsumeAndReturnRemaining(1);
        if (!intento.isConsumed()) {
            long segundos = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(intento.getNanosToWaitForRefill()));
            respuesta.setHeader("Retry-After", String.valueOf(segundos));
            respuestaError.escribir(respuesta, 429, MENSAJE_LIMITE);
            return;
        }
        cadena.doFilter(peticion, respuesta);
    }

    private Bucket nuevoContador() {
        return Bucket.builder()
                .addLimit(limite -> limite.capacity(limitePorMinuto)
                        .refillGreedy(limitePorMinuto, Duration.ofMinutes(1)))
                .build();
    }

    private static String cliente(HttpServletRequest peticion) {
        String clave = peticion.getHeader(SecurityFilter.ENCABEZADO);
        return resumen(clave == null ? "" : clave) + "|" + peticion.getRemoteAddr();
    }

    private static String resumen(String valor) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no tiene SHA-256", e);
        }
    }
}
