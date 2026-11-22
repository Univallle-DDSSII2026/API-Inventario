package co.edu.univalle.inventario.dto;

/**
 * Orden en que se aplican las validaciones del DTO: primero obligatoriedad, luego longitud y al final los caracteres
 * permitidos. Así cada petición inválida recibe un solo mensaje, el del primer criterio incumplido.
 */
public interface OrdenValidacion {

    /** Primer grupo: el campo no puede venir vacío. */
    interface Obligatorio {
    }

    /** Segundo grupo: la longitud del campo. */
    interface Longitud {
    }

    /** Tercer grupo: los caracteres permitidos. */
    interface Caracteres {
    }
}