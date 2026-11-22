package co.edu.univalle.inventario.dto;

/**
 * Cuerpo de las respuestas de error (View Layer; DAS v1.3, Tabla 11): código HTTP y el mensaje exacto del criterio.
 *
 * @param codigo código HTTP de la respuesta
 * @param mensaje mensaje para el usuario
 */
public record ErrorResponse(int codigo, String mensaje) {
}