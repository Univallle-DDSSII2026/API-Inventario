package co.edu.univalle.inventario.dto;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /inventario/articulos-terceros} (View Layer, HU_CUTIT_14). También es el cuerpo de
 * {@code POST /inventario/productos-terminados} (HU_CUTIT_01), que tiene los mismos campos y reglas. Las
 * restricciones de Bean Validation se aplican sobre el texto ya ajustado: sin espacios al inicio ni al final y con
 * los espacios repetidos reducidos a uno.
 *
 * @param nombre nombre del artículo (obligatorio, de 3 a 100 caracteres)
 * @param descripcion descripción del artículo (opcional, hasta 500 caracteres)
 */
@GroupSequence({
    OrdenValidacion.Obligatorio.class,
    OrdenValidacion.Longitud.class,
    OrdenValidacion.Caracteres.class,
    RegistrarArticuloTercerosDto.class
})
public record RegistrarArticuloTercerosDto(
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
        String descripcion) {

    /**
     * Ajusta los espacios de los dos campos apenas se crea el DTO, antes de validarlo.
     *
     * @param nombre nombre recibido
     * @param descripcion descripción recibida
     */
    public RegistrarArticuloTercerosDto {
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