package co.edu.univalle.inventario.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ClavesApiTest {

    @Test
    void deberia_encontrar_el_rol_de_cada_clave() {
        // Arrange
        ClavesApi claves = ClavesApi.desde("clave-a:FABRICACION, clave-b:EQUIPO");

        // Act y Assert
        assertThat(claves.rolDe("clave-a")).contains(RolCliente.FABRICACION);
        assertThat(claves.rolDe("clave-b")).contains(RolCliente.EQUIPO);
        assertThat(claves.rolDe("clave-c")).isEmpty();
    }

    @Test
    void deberia_rechazar_una_configuracion_vacia() {
        assertThatThrownBy(() -> ClavesApi.desde(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("API_KEYS no tiene ninguna clave");
        assertThatThrownBy(() -> ClavesApi.desde(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deberia_rechazar_una_clave_sin_rol_o_con_un_rol_que_no_existe() {
        assertThatThrownBy(() -> ClavesApi.desde("clave-sin-rol"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cada clave de API_KEYS debe tener el formato clave:ROL");
        assertThatThrownBy(() -> ClavesApi.desde(":EQUIPO")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ClavesApi.desde("clave:ADMIN")).isInstanceOf(IllegalArgumentException.class);
    }
}
