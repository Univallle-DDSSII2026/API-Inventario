package co.edu.univalle.inventario.filter;

/**
 * Sistema cliente que llama a la API; cada uno tiene su propia API Key (DAS v1.3, sección 2.1, Seguridad).
 */
public enum RolCliente {
    /** API de Fabricación (Equipo 3). */
    FABRICACION,
    /** API de Notificaciones (Equipo 5). */
    NOTIFICACIONES,
    /** Equipo 4, desde Swagger UI. */
    EQUIPO
}
