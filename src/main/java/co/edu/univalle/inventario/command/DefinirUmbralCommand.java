package co.edu.univalle.inventario.command;

/**
 * Datos que la Logic Layer necesita para definir el Umbral mínimo de un artículo (HU_CUTIT_06). Lo crea el
 * controlador a partir de la ruta y del DTO.
 *
 * @param codigo Código del artículo recibido en la ruta
 * @param umbralMinimo Umbral mínimo recibido en el cuerpo, como texto, o null si no se envió
 */
public record DefinirUmbralCommand(String codigo, String umbralMinimo) {
}