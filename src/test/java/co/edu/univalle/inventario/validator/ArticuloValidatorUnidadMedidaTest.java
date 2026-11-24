package co.edu.univalle.inventario.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import co.edu.univalle.inventario.exception.SolicitudInvalidaException;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Prueba los criterios 6 y 7 de HU_CUTIT_11 en el Validator, con el mensaje exacto de cada uno. No necesita base de
 * datos: la validación solo depende del texto recibido.
 */
class ArticuloValidatorUnidadMedidaTest {

    private final ArticuloValidator validator = new ArticuloValidator(mock(ArticuloRepository.class));

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void deberia_exigir_la_unidad_de_medida(String unidad) {
        assertThatThrownBy(() -> validator.validarUnidadMedida(unidad))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El campo Unidad de medida es obligatorio");
    }

    @ParameterizedTest
    @ValueSource(strings = {"und", "g", "kg", "mL", "L", "m"})
    void deberia_aceptar_las_unidades_de_la_lista(String unidad) {
        assertThatCode(() -> validator.validarUnidadMedida(unidad)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ml", "l", "KG", "M", " kg", "kg ", "litros", "una unidad demasiado larga"})
    void deberia_rechazar_una_unidad_que_no_esta_en_la_lista(String unidad) {
        assertThatThrownBy(() -> validator.validarUnidadMedida(unidad))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("La Unidad de medida %s no es válida".formatted(unidad));
    }
}