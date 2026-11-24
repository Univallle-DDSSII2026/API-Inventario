package co.edu.univalle.inventario.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class RegistrarIngresoFabricacionDtoTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void deberia_conservar_los_digitos_de_la_cantidad_json() {
        var dto = json.readValue("""
                {"codigo":"PT-00001","cantidad":1.00000000000000000001,"fecha":"29/02/2024",
                "referenciaFabricacion":"  Lote ✓  "}
                """, RegistrarIngresoFabricacionDto.class);
        assertThat(dto.cantidad()).isEqualTo("1.00000000000000000001");
        assertThat(dto.referenciaFabricacion()).isEqualTo("  Lote ✓  ");
    }

    @Test
    void deberia_distinguir_una_cantidad_vacia_de_una_cantidad_no_numerica() {
        var ausente = json.readValue("{}", RegistrarIngresoFabricacionDto.class);
        var nula = json.readValue("{\"cantidad\":null}", RegistrarIngresoFabricacionDto.class);
        var vacia = json.readValue("{\"cantidad\":\" \"}", RegistrarIngresoFabricacionDto.class);
        var invalida = json.readValue("""
                {"cantidad":[1,2],"fecha":"29/02/2024"}
                """, RegistrarIngresoFabricacionDto.class);
        assertThat(ausente.cantidad()).isNull();
        assertThat(nula.cantidad()).isNull();
        assertThat(vacia.cantidad()).isEqualTo(" ");
        assertThat(invalida.cantidad()).isNotBlank();
        assertThat(invalida.fecha()).isEqualTo("29/02/2024");
    }
}
