package co.edu.univalle.inventario.command;

/**
 * Datos que la Logic Layer necesita para registrar una materia prima (HU_CUTIT_11). Lo crea el controlador a partir
 * del DTO, ya con los espacios del nombre y la descripción ajustados (DAS v1.3: los controladores pasan comandos a
 * los Service).
 *
 * @param nombre nombre de la materia prima
 * @param descripcion descripción de la materia prima, o null si no se envió
 * @param unidadMedida unidad de medida recibida, como texto, o null si no se envió
 */
public record RegistrarMateriaPrimaCommand(String nombre, String descripcion, String unidadMedida) {
}