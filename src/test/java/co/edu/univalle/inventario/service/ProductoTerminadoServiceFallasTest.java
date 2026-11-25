package co.edu.univalle.inventario.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.univalle.inventario.command.RegistrarArticuloTercerosCommand;
import co.edu.univalle.inventario.exception.NombreDuplicadoException;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.Consecutivo;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import co.edu.univalle.inventario.repository.ConsecutivoRepository;
import co.edu.univalle.inventario.validator.ArticuloValidator;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Prueba de las fallas de ArticuloService.registrarProductoTerminado con dobles de Mockito: un registro simultáneo que
 * el índice único de la base de datos rechaza y un consecutivo PT que no existe. Ninguna se puede provocar con la
 * base real sin afectar las demás pruebas.
 */
@ExtendWith(MockitoExtension.class)
class ProductoTerminadoServiceFallasTest {

    @Mock
    private ArticuloRepository articuloRepository;

    @Mock
    private ConsecutivoRepository consecutivoRepository;

    @Mock
    private ArticuloValidator articuloValidator;

    @InjectMocks
    private ArticuloService articuloService;

    @Test
    void deberia_responder_nombre_duplicado_si_el_indice_unico_rechaza_un_registro_simultaneo() {
        // Arrange
        Consecutivo consecutivo = mock(Consecutivo.class);
        when(consecutivo.siguiente()).thenReturn("PT-00003");
        when(consecutivoRepository.findByPrefijo("PT")).thenReturn(Optional.of(consecutivo));
        when(articuloRepository.saveAndFlush(any(Articulo.class)))
                .thenThrow(new DataIntegrityViolationException("uk_articulo_nombre_lower"));

        // Act & Assert
        assertThatThrownBy(() -> articuloService.registrarProductoTerminado(
                new RegistrarArticuloTercerosCommand("Cadena", null)))
                .isInstanceOf(NombreDuplicadoException.class)
                .hasMessage("Ya existe un artículo con el nombre Cadena");
    }

    @Test
    void deberia_fallar_sin_guardar_nada_si_no_existe_el_consecutivo_del_prefijo_PT() {
        // Arrange
        when(consecutivoRepository.findByPrefijo("PT")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> articuloService.registrarProductoTerminado(
                new RegistrarArticuloTercerosCommand("Cadena", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No existe el consecutivo del prefijo PT");
        verifyNoInteractions(articuloRepository);
    }
}