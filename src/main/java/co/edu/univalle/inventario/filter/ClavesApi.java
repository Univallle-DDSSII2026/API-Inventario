package co.edu.univalle.inventario.filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * API Keys configuradas y el rol de cada una. Se leen de la variable API_KEYS con el formato
 * {@code clave:ROL,clave:ROL} (DAS v1.3, Tabla 14).
 */
public final class ClavesApi {

    private final Map<String, RolCliente> roles;

    private ClavesApi(Map<String, RolCliente> roles) {
        this.roles = roles;
    }

    /**
     * Interpreta el valor de API_KEYS.
     * @param configuracion texto con el formato {@code clave:ROL,clave:ROL}
     * @return las claves con su rol
     * @throws IllegalArgumentException si el valor está vacío o mal escrito
     */
    public static ClavesApi desde(String configuracion) {
        if (configuracion == null || configuracion.isBlank()) {
            throw new IllegalArgumentException("API_KEYS no tiene ninguna clave");
        }
        Map<String, RolCliente> roles = new LinkedHashMap<>();
        for (String par : configuracion.split(",")) {
            String[] partes = par.trim().split(":");
            if (partes.length != 2 || partes[0].isBlank()) {
                throw new IllegalArgumentException("Cada clave de API_KEYS debe tener el formato clave:ROL");
            }
            roles.put(partes[0].trim(), RolCliente.valueOf(partes[1].trim()));
        }
        return new ClavesApi(roles);
    }

    /**
     * Busca el rol de una API Key comparando en tiempo constante, para no revelar claves por el tiempo de respuesta.
     * @param clave valor del encabezado X-API-Key
     * @return el rol, o vacío si la clave no existe
     */
    public Optional<RolCliente> rolDe(String clave) {
        byte[] recibida = clave.getBytes(StandardCharsets.UTF_8);
        RolCliente encontrado = null;
        for (Map.Entry<String, RolCliente> entrada : roles.entrySet()) {
            if (MessageDigest.isEqual(recibida, entrada.getKey().getBytes(StandardCharsets.UTF_8))) {
                encontrado = entrada.getValue();
            }
        }
        return Optional.ofNullable(encontrado);
    }
}
