package co.edu.univalle.inventario.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

/**
 * Prueba las restricciones del DTO de HU_CUTIT_14 (criterios 1 a 5): cada caso inválido recibe el mensaje exacto.
 */
class RegistrarArticuloTercerosDtoTest {

    private static final String NOMBRE_OBLIGATORIO = "El campo Nombre es obligatorio";
    private static final String NOMBRE_LONGITUD = "El Nombre debe tener entre 3 y 100 caracteres";
    private static final String NOMBRE_CARACTERES = "El Nombre contiene caracteres no permitidos";
    private static final String DESCRIPCION_LONGITUD = "La Descripción no puede superar 500 caracteres";
    private static final String DESCRIPCION_CARACTERES = "La Descripción contiene caracteres no permitidos";

    /** Devuelve el primer mensaje de error del DTO, o null si es válido. */
    private String primerMensaje(String nombre, String descripcion) {
        try (ValidatorFactory fabrica = Validation.buildDefaultValidatorFactory()) {
            return fabrica.getValidator().validate(new RegistrarArticuloTercerosDto(nombre, descripcion)).stream()
                    .map(ConstraintViolation::getMessage)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Test
    void deberia_aceptar_nombre_y_descripcion_con_los_caracteres_permitidos() {
        assertThat(primerMensaje("Kit cadena 3/8, 1.1 mm #2 (Ñandú) - tipo A", "Repuesto (ñ, á, 10/12) #1")).isNull();
    }

    @Test
    void deberia_aceptar_la_descripcion_vacia_o_ausente() {
        assertThat(primerMensaje("Motosierra", null)).isNull();
        assertThat(primerMensaje("Motosierra", "")).isNull();
    }

    @Test
    void deberia_ajustar_los_espacios_del_nombre_y_de_la_descripcion() {
        // Act
        RegistrarArticuloTercerosDto dto =
                new RegistrarArticuloTercerosDto("  Motosierra    Stihl  ", " Con   espada ");

        // Assert
        assertThat(dto.nombre()).isEqualTo("Motosierra Stihl");
        assertThat(dto.descripcion()).isEqualTo("Con espada");
    }

    @Test
    void deberia_rechazar_el_nombre_nulo_vacio_o_solo_con_espacios() {
        assertThat(primerMensaje(null, null)).isEqualTo(NOMBRE_OBLIGATORIO);
        assertThat(primerMensaje("", null)).isEqualTo(NOMBRE_OBLIGATORIO);
        assertThat(primerMensaje("     ", null)).isEqualTo(NOMBRE_OBLIGATORIO);
    }

    @Test
    void deberia_rechazar_el_nombre_de_menos_de_3_o_mas_de_100_caracteres() {
        assertThat(primerMensaje("ab", null)).isEqualTo(NOMBRE_LONGITUD);
        assertThat(primerMensaje("  ab   ", null)).isEqualTo(NOMBRE_LONGITUD);
        assertThat(primerMensaje("a".repeat(101), null)).isEqualTo(NOMBRE_LONGITUD);
    }

    @Test
    void deberia_aceptar_el_nombre_de_exactamente_3_y_de_exactamente_100_caracteres() {
        assertThat(primerMensaje("abc", null)).isNull();
        assertThat(primerMensaje("a".repeat(100), null)).isNull();
    }

    @Test
    void deberia_rechazar_el_nombre_con_caracteres_no_permitidos() {
        assertThat(primerMensaje("Motosierra @", null)).isEqualTo(NOMBRE_CARACTERES);
        assertThat(primerMensaje("Kit$ cadena", null)).isEqualTo(NOMBRE_CARACTERES);
    }

    @Test
    void deberia_rechazar_la_descripcion_de_mas_de_500_caracteres() {
        assertThat(primerMensaje("Motosierra", "a".repeat(501))).isEqualTo(DESCRIPCION_LONGITUD);
        assertThat(primerMensaje("Motosierra", "a".repeat(500))).isNull();
    }

    @Test
    void deberia_rechazar_la_descripcion_con_caracteres_no_permitidos() {
        assertThat(primerMensaje("Motosierra", "Incluye * extra")).isEqualTo(DESCRIPCION_CARACTERES);
    }
}