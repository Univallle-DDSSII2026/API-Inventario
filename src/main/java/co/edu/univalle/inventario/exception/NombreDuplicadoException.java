package co.edu.univalle.inventario.exception;

/**
 * Ya existe un artículo (de cualquier tipo) con ese nombre. El GlobalExceptionHandler la convierte en 409.
 */
public class NombreDuplicadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con el mensaje exacto del criterio de aceptación.
     *
     * @param nombre nombre que ya está en uso
     */
    public NombreDuplicadoException(String nombre) {
        super("Ya existe un artículo con el nombre " + nombre);
    }
}