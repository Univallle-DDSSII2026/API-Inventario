package co.edu.univalle.inventario.exception;

/**
 * La solicitud incumple una regla de validación (campo vacío, formato o valor inválido). El GlobalExceptionHandler la
 * convierte en 400 con el mensaje exacto del criterio de aceptación.
 */
public class SolicitudInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con el mensaje exacto del criterio de aceptación.
     *
     * @param mensaje mensaje para el usuario
     */
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}