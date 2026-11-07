package co.edu.univalle.inventario.exception;

import java.util.List;

/**
 * Todas las líneas fueron rechazadas: no se registra nada y se responde 422 (HU_CUTIT_04, criterio 9).
 */
public class SolicitudRechazadaException extends RuntimeException {

    private final List<String> lineasRechazadas;

    /**
     * Crea el rechazo con el detalle de cada línea.
     * @param mensaje mensaje general
     * @param lineasRechazadas motivo de rechazo de cada línea
     */
    public SolicitudRechazadaException(String mensaje, List<String> lineasRechazadas) {
        super(mensaje);
        this.lineasRechazadas = List.copyOf(lineasRechazadas);
    }

    /**
     * Devuelve el motivo de rechazo de cada línea.
     * @return lista inmutable de motivos
     */
    public List<String> getLineasRechazadas() {
        return lineasRechazadas;
    }
}
