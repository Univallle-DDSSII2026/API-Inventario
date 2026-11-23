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
     * Arma la respuesta de un artículo recién registrado (criterio 7 de HU_CUTIT_01 y HU_CUTIT_14).
     *
     * @param articulo artículo guardado
     * @return la respuesta con el mensaje "Se registró el artículo [Código] – [Nombre] con Tipo [Tipo]", donde
     *         [Tipo] es, por ejemplo, "Producto terminado"
     */
    public static RegistroArticuloDto de(Articulo articulo) {
        String mensaje = "Se registró el artículo %s – %s con Tipo %s"
                .formatted(articulo.getCodigo(), articulo.getNombre(), articulo.getTipo().getEtiqueta());
        return new RegistroArticuloDto(mensaje, ArticuloDto.de(articulo));
    }
}