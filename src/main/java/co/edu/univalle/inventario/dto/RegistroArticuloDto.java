package co.edu.univalle.inventario.dto;

import co.edu.univalle.inventario.model.Articulo;

/**
 * Respuesta de un registro exitoso (View Layer): el mensaje del criterio de aceptación y el artículo creado.
 *
 * @param mensaje mensaje de confirmación
 * @param articulo artículo registrado
 */
public record RegistroArticuloDto(String mensaje, ArticuloDto articulo) {

    /**
     * Arma la respuesta de un artículo de terceros recién registrado (HU_CUTIT_14, criterio 7).
     *
     * @param articulo artículo guardado
     * @return la respuesta con el mensaje "Se registró el artículo [Código] – [Nombre] con Tipo Artículo de terceros"
     */
    public static RegistroArticuloDto de(Articulo articulo) {
        String mensaje = "Se registró el artículo %s – %s con Tipo Artículo de terceros"
                .formatted(articulo.getCodigo(), articulo.getNombre());
        return new RegistroArticuloDto(mensaje, ArticuloDto.de(articulo));
    }
}