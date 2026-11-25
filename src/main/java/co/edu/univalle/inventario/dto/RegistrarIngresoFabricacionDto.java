package co.edu.univalle.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Cuerpo del ingreso. Conserva los valores JSON hasta que puedan validarse por campo. */
public record RegistrarIngresoFabricacionDto(
        @Schema(type = "string", example = "PT-00001") Object codigo,
        @Schema(type = "integer", example = "3")
        @JsonDeserialize(using = CantidadDeserializer.class) String cantidad,
        @Schema(type = "string", example = "23/11/2026") Object fecha,
        @Schema(type = "string", maxLength = 30, example = "LOTE-42") Object referenciaFabricacion) {

    /** Lee la cantidad sin convertir los números JSON a punto flotante. */
    public static class CantidadDeserializer extends ValueDeserializer<String> {

        @Override
        public String deserialize(JsonParser parser, DeserializationContext contexto) {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_STRING || token == JsonToken.VALUE_NUMBER_INT
                    || token == JsonToken.VALUE_NUMBER_FLOAT) {
                return parser.getString();
            }
            parser.skipChildren();
            return "no numérico";
        }
    }
}
