package co.edu.univalle.inventario.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

class SecurityFilterTest {

    private final SecurityFilter filtro = new SecurityFilter(
            ClavesApi.desde("clave-fabricacion:FABRICACION"), new RespuestaError(JsonMapper.builder().build()));

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest peticion(String clave) {
        MockHttpServletRequest peticion = new MockHttpServletRequest("POST", "/inventario/salidas");
        if (clave != null) {
            peticion.addHeader(SecurityFilter.ENCABEZADO, clave);
        }
        return peticion;
    }

    @Test
    void deberia_asignar_el_rol_de_la_api_key_valida() throws Exception {
        // Arrange
        MockFilterChain cadena = new MockFilterChain();

        // Act
        filtro.doFilter(peticion("clave-fabricacion"), new MockHttpServletResponse(), cadena);

        // Assert
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertThat(cadena.getRequest()).isNotNull();
        assertThat(autenticacion.getName()).isEqualTo("FABRICACION");
        assertThat(autenticacion.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_FABRICACION");
    }

    @Test
    void deberia_responder_401_si_la_api_key_no_existe() throws Exception {
        // Arrange
        MockFilterChain cadena = new MockFilterChain();
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        // Act
        filtro.doFilter(peticion("clave-falsa"), respuesta, cadena);

        // Assert
        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(respuesta.getContentAsString()).contains(SecurityFilter.MENSAJE_NO_AUTORIZADO);
        assertThat(cadena.getRequest()).isNull();
    }

    @Test
    void deberia_dejar_seguir_sin_autenticar_si_no_hay_api_key() throws Exception {
        // Arrange
        MockFilterChain sinEncabezado = new MockFilterChain();
        MockFilterChain enBlanco = new MockFilterChain();

        // Act
        filtro.doFilter(peticion(null), new MockHttpServletResponse(), sinEncabezado);
        filtro.doFilter(peticion(" "), new MockHttpServletResponse(), enBlanco);

        // Assert: Spring Security decide después si la ruta la exige (401) o es pública.
        assertThat(sinEncabezado.getRequest()).isNotNull();
        assertThat(enBlanco.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
