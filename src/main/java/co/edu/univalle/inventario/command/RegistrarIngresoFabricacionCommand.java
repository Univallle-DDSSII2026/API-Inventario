package co.edu.univalle.inventario.command;

/** Valores recibidos del operario, pendientes de validación. */
public record RegistrarIngresoFabricacionCommand(Object codigo, String cantidad, Object fecha,
        Object referenciaFabricacion) {
}
