package co.edu.univalle.inventario.dto;

import java.util.List;
import java.util.UUID;

/**
 * Respuesta 201 de una salida con al menos una línea registrada (HU_CUTIT_04, criterio 8).
 */
public record SalidaResponse(UUID solicitudId, String mensaje, int lineasRegistradas, List<String> lineasRechazadas) {
}
