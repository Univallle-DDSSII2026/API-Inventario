package co.edu.univalle.inventario.exception;

/**
 * No existe un artículo con el código recibido. El GlobalExceptionHandler la convierte en 404.
 */
public class ArticuloNoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con el mensaje exacto del criterio de aceptación.
     *
     * @param codigo código que no existe
     */
    public ArticuloNoEncontradoException(String codigo) {
        super("No existe un artículo con el código " + codigo);
    }
}