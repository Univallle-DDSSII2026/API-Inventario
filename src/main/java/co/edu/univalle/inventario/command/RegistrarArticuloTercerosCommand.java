package co.edu.univalle.inventario.command;

/**
 * Datos que la Logic Layer necesita para registrar un artículo de terceros (HU_CUTIT_14) o un producto terminado
 * (HU_CUTIT_01). Lo crea el controlador a partir del DTO, ya con los espacios ajustados (DAS v1.3: los controladores
 * pasan comandos a los Service).
 *
 * @param nombre nombre del artículo
 * @param descripcion descripción del artículo, o null si no se envió
 */
public record RegistrarArticuloTercerosCommand(String nombre, String descripcion) {
}