package co.edu.univalle.inventario.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Prueba el ajuste de espacios y las validaciones de texto del cuerpo de HU_CUTIT_11. La Unidad de medida no se valida
 * aquí: la valida la Logic Layer, después de estas reglas.
 */
class RegistrarMateriaPrimaDtoTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static List<String> mensajes(RegistrarMateriaPrimaDto dto) {
        return VALIDATOR.validate(dto).stream().map(ConstraintViolation::getMessage).toList();
    }

    @Test
    void deberia_ajustar_los_espacios_del_nombre_y_la_descripcion() {
        RegistrarMateriaPrimaDto dto = new RegistrarMateriaPrimaDto("  Acero   templado  ", " Para  cadenas ", "kg");

        assertThat(dto.nombre()).isEqualTo("Acero templado");
        assertThat(dto.descripcion()).isEqualTo("Para cadenas");
        assertThat(dto.unidadMedida()).isEqualTo("kg");
        assertThat(mensajes(dto)).isEmpty();
    }

    @Test
    void deberia_aceptar_una_descripcion_ausente() {
        assertThat(mensajes(new RegistrarMateriaPrimaDto("Aceite (SAE-30), 1/2 #3.", null, "L"))).isEmpty();
    }

    @Test
    void deberia_exigir_el_nombre() {
        assertThat(mensajes(new RegistrarMateriaPrimaDto(null, null, "kg")))
                .containsExactly("El campo Nombre es obligatorio");
        assertThat(mensajes(new RegistrarMateriaPrimaDto("   ", null, "kg")))
                .containsExactly("El campo Nombre es obligatorio");
    }

    @Test
    void deberia_validar_la_longitud_del_nombre() {
        assertThat(mensajes(new RegistrarMateriaPrimaDto("ab", null, "kg")))
                .containsExactly("El Nombre debe tener entre 3 y 100 caracteres");
        assertThat(mensajes(new RegistrarMateriaPrimaDto("a".repeat(101), null, "kg")))
                .containsExactly("El Nombre debe tener entre 3 y 100 caracteres");
    }

    @Test
    void deberia_rechazar_caracteres_no_permitidos_en_el_nombre() {
        assertThat(mensajes(new RegistrarMateriaPrimaDto("Acero@", null, "kg")))
                .containsExactly("El Nombre contiene caracteres no permitidos");
    }

    @Test
    void deberia_validar_la_descripcion() {
        assertThat(mensajes(new RegistrarMateriaPrimaDto("Acero", "a".repeat(501), "kg")))
                .containsExactly("La Descripción no puede superar 500 caracteres");
        assertThat(mensajes(new RegistrarMateriaPrimaDto("Acero", "Texto $", "kg")))
                .containsExactly("La Descripción contiene caracteres no permitidos");
    }

    @Test
    void deberia_rechazar_el_nombre_vacio_aunque_la_unidad_sea_invalida() {
        assertThat(mensajes(new RegistrarMateriaPrimaDto("", null, "litros")))
                .containsExactly("El campo Nombre es obligatorio");
    }
}