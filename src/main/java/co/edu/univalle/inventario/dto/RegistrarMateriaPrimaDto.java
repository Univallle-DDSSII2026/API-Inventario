package co.edu.univalle.inventario.dto;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /inventario/materias-primas} (View Layer, HU_CUTIT_11). Nombre y Descripción siguen las
 * mismas reglas que en HU_CUTIT_01 y HU_CUTIT_14 y se validan sobre el texto ya ajustado: sin espacios al inicio ni
 * al final y con los espacios repetidos reducidos a uno. La Unidad de medida llega como texto y la valida la Logic
 * Layer, para responder con el mensaje exacto y después de las validaciones de texto.
 *
 * @param nombre nombre de la materia prima (obligatorio, de 3 a 100 caracteres)
 * @param descripcion descripción de la materia prima (opcional, hasta 500 caracteres)
 * @param unidadMedida unidad de medida (obligatoria: und, g, kg, mL, L o m)
 */
@GroupSequence({
    OrdenValidacion.Obligatorio.class,
    OrdenValidacion.Longitud.class,
    OrdenValidacion.Caracteres.class,
    RegistrarMateriaPrimaDto.class
})
public record RegistrarMateriaPrimaDto(
        @NotBlank(message = "El campo Nombre es obligatorio", groups = OrdenValidacion.Obligatorio.class)
        @Size(min = 3, max = 100, message = "El Nombre debe tener entre 3 y 100 caracteres",
                groups = OrdenValidacion.Longitud.class)
        @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜáéíóúüÑñ0-9 .,/()#-]*$",
                message = "El Nombre contiene caracteres no permitidos", groups = OrdenValidacion.Caracteres.class)
        String nombre,

        @Size(max = 500, message = "La Descripción no puede superar 500 caracteres",
                groups = OrdenValidacion.Longitud.class)
        @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜáéíóúüÑñ0-9 .,/()#-]*$",
                message = "La Descripción contiene caracteres no permitidos",
                groups = OrdenValidacion.Caracteres.class)
        String descripcion,

        String unidadMedida) {

    /**
     * Ajusta los espacios del nombre y de la descripción apenas se crea el DTO, antes de validarlo.
     *
     * @param nombre nombre recibido
     * @param descripcion descripción recibida
     * @param unidadMedida unidad de medida recibida, que no se modifica
     */
    public RegistrarMateriaPrimaDto {
        nombre = ajustarEspacios(nombre);
        descripcion = ajustarEspacios(descripcion);
    }

    private static String ajustarEspacios(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.strip().replaceAll(" {2,}", " ");
    }
}