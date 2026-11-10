package co.edu.univalle.inventario.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autenticador de la View Layer: valida la API Key del encabezado X-API-Key y asigna el rol del sistema cliente
 * (DAS v1.3, Tabla 10). Sin clave deja seguir la petición: si la ruta la exige, Spring Security responde 401.
 */
public class SecurityFilter extends OncePerRequestFilter {

    /** Encabezado con la API Key. */
    public static final String ENCABEZADO = "X-API-Key";

    /** Mensaje del 401 (Tabla 11). */
    public static final String MENSAJE_NO_AUTORIZADO = "Acceso no autorizado. Verifique la API Key.";

    private final ClavesApi claves;
    private final RespuestaError respuestaError;

    /**
     * Crea el filtro.
     * @param claves API Keys configuradas
     * @param respuestaError escritor del cuerpo de error
     */
    public SecurityFilter(ClavesApi claves, RespuestaError respuestaError) {
        this.claves = claves;
        this.respuestaError = respuestaError;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String clave = peticion.getHeader(ENCABEZADO);
        if (clave == null || clave.isBlank()) {
            cadena.doFilter(peticion, respuesta);
            return;
        }
        Optional<RolCliente> rol = claves.rolDe(clave);
        if (rol.isEmpty()) {
            respuestaError.escribir(respuesta, HttpServletResponse.SC_UNAUTHORIZED, MENSAJE_NO_AUTORIZADO);
            return;
        }
        UsernamePasswordAuthenticationToken autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                rol.get().name(), null, List.of(new SimpleGrantedAuthority("ROLE_" + rol.get().name())));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
        cadena.doFilter(peticion, respuesta);
    }
}
