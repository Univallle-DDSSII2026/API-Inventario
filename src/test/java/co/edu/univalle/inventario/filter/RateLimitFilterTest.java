package co.edu.univalle.inventario.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class RateLimitFilterTest {

    private final RateLimitFilter filtro = new RateLimitFilter(2, new RespuestaError(JsonMapper.builder().build()));

    private MockHttpServletResponse llamar(String ruta, String clave, String ip) throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", ruta);
        peticion.setRemoteAddr(ip);
        if (clave != null) {
            peticion.addHeader(SecurityFilter.ENCABEZADO, clave);
        }
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(peticion, respuesta, new MockFilterChain());
        return respuesta;
    }

    @Test
    void deberia_responder_429_al_superar_el_limite_del_cliente() throws Exception {
        // Arrange: límite de 2 peticiones por minuto.
        llamar("/inventario/salidas", "clave-a", "10.0.0.1");
        llamar("/inventario/salidas", "clave-a", "10.0.0.1");

        // Act
        MockHttpServletResponse tercera = llamar("/inventario/salidas", "clave-a", "10.0.0.1");

        // Assert
        assertThat(tercera.getStatus()).isEqualTo(429);
        assertThat(tercera.getHeader("Retry-After")).isNotNull();
        assertThat(tercera.getContentAsString()).contains(RateLimitFilter.MENSAJE_LIMITE);
    }

    @Test
    void deberia_contar_por_separado_cada_clave_y_cada_ip() throws Exception {
        // Arrange: el cliente "clave-a" desde 10.0.0.1 ya gastó su límite.
        llamar("/inventario/salidas", "clave-a", "10.0.0.1");
        llamar("/inventario/salidas", "clave-a", "10.0.0.1");

        // Act y Assert: otra IP, otra clave y una petición sin clave tienen su propio contador.
        assertThat(llamar("/inventario/salidas", "clave-a", "10.0.0.2").getStatus()).isEqualTo(200);
        assertThat(llamar("/inventario/salidas", "clave-b", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(llamar("/inventario/salidas", null, "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void no_deberia_limitar_el_health_check() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(llamar("/actuator/health", null, "10.0.0.9").getStatus()).isEqualTo(200);
        }
    }
}
