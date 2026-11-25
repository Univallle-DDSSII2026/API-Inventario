package co.edu.univalle.inventario.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class RegistrarIngresoCompraTercerosDtoTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void deberia_conservar_los_digitos_de_cantidad_y_costo_unitario() {
        var dto = json.readValue("""
                {"codigo":"AT-00001","cantidad":5,"fecha":"24/11/2026",
                "numeroFactura":"FAC-001","proveedor":"Proveedor S.A.",
                "costoUnitario":1250000.50}
                """, RegistrarIngresoCompraTercerosDto.class);
        assertThat(dto.codigo()).isEqualTo("AT-00001");
        assertThat(dto.cantidad()).isEqualTo("5");
        assertThat(dto.fecha()).isEqualTo("24/11/2026");
        assertThat(dto.numeroFactura()).isEqualTo("FAC-001");
        assertThat(dto.proveedor()).isEqualTo("Proveedor S.A.");
        assertThat(dto.costoUnitario()).isEqualTo("1250000.50");
    }

    @Test
    void deberia_distinguir_campos_vacios_de_valores_no_numericos() {
        var ausente = json.readValue("{}", RegistrarIngresoCompraTercerosDto.class);
        var nulos = json.readValue("{\"cantidad\":null,\"costoUnitario\":null}",
                RegistrarIngresoCompraTercerosDto.class);
        var vacios = json.readValue("{\"cantidad\":\" \",\"costoUnitario\":\" \"}",
                RegistrarIngresoCompraTercerosDto.class);
        var invalidos = json.readValue("""
                {"cantidad":[1,2],"costoUnitario":{"valor":100}}
                """, RegistrarIngresoCompraTercerosDto.class);

        assertThat(ausente.cantidad()).isNull();
        assertThat(ausente.costoUnitario()).isNull();
        assertThat(nulos.cantidad()).isNull();
        assertThat(nulos.costoUnitario()).isNull();
        assertThat(vacios.cantidad()).isEqualTo(" ");
        assertThat(vacios.costoUnitario()).isEqualTo(" ");
        assertThat(invalidos.cantidad()).isNotBlank();
        assertThat(invalidos.costoUnitario()).isNotBlank();
    }
}
