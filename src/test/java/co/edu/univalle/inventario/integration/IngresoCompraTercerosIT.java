package co.edu.univalle.inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.univalle.inventario.dto.ErrorResponse;
import co.edu.univalle.inventario.dto.IngresoCompraTercerosRegistradoDto;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class IngresoCompraTercerosIT {

    private static final String RUTA = "/inventario/ingresos/compras/articulos-terceros";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.9-alpine"));

    static {
        POSTGRES.start();
    }

    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${local.server.port}")
    private int puerto;

    private LocalDate hoy;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM movimiento");
        jdbc.update("""
                INSERT INTO articulo (codigo, nombre, tipo, existencias, umbral_minimo)
                VALUES ('AT-91001', 'Motosierra HU13', 'ARTICULO_TERCEROS', 0, 2)
                ON CONFLICT (codigo) DO UPDATE SET existencias = 0
                """);
        hoy = LocalDate.now(ZoneId.of("America/Bogota"));
    }

    @Test
    void deberia_registrar_ingreso_por_compra_y_actualizar_existencias() {
        RespuestaHttp primera = enviar(cuerpoValido());
        assertThat(primera.estado()).isEqualTo(201);
        IngresoCompraTercerosRegistradoDto respuesta1 = json.readValue(
                primera.cuerpo(), IngresoCompraTercerosRegistradoDto.class);
        assertThat(respuesta1.mensaje())
                .isEqualTo("Ingreso registrado: 5 de AT-91001 – Motosierra HU13. Existencias actuales: 5");

        Map<String, Object> movimiento = jdbc.queryForMap("""
                SELECT tipo, origen, cantidad, fecha, referencia, proveedor, costo_unitario,
                       solicitud_id, fecha_registro
                FROM movimiento
                """);
        assertThat(movimiento.get("tipo")).isEqualTo("Ingreso");
        assertThat(movimiento.get("origen")).isEqualTo("Compra");
        assertThat((BigDecimal) movimiento.get("cantidad")).isEqualByComparingTo("5");
        assertThat(((Date) movimiento.get("fecha")).toLocalDate()).isEqualTo(hoy);
        assertThat(movimiento.get("referencia")).isEqualTo("FAC-2026-001");
        assertThat(movimiento.get("proveedor")).isEqualTo("Distribuidora Industrial S.A.");
        assertThat((BigDecimal) movimiento.get("costo_unitario")).isEqualByComparingTo("1250000.50");
        assertThat(movimiento.get("solicitud_id")).isNotNull();
        assertThat(movimiento.get("fecha_registro")).isNotNull();
        assertThat(existenciasDe("AT-91001")).isEqualByComparingTo("5");

        Map<String, Object> segundaCarga = cuerpoValido();
        segundaCarga.put("cantidad", 3);
        segundaCarga.put("fecha", hoy.minusDays(30).format(FORMATO_FECHA));
        segundaCarga.put("costoUnitario", 980000);

        RespuestaHttp segunda = enviar(segundaCarga);
        assertThat(segunda.estado()).isEqualTo(201);
        IngresoCompraTercerosRegistradoDto respuesta2 = json.readValue(
                segunda.cuerpo(), IngresoCompraTercerosRegistradoDto.class);
        assertThat(respuesta2.mensaje())
                .isEqualTo("Ingreso registrado: 3 de AT-91001 – Motosierra HU13. Existencias actuales: 8");
        assertThat(existenciasDe("AT-91001")).isEqualByComparingTo("8");
        assertThat(contarMovimientos()).isEqualTo(2);
    }

    @Test
    void deberia_rechazar_solicitud_sin_cuerpo() {
        RespuestaHttp respuesta = enviarCrudo(null);
        verificarError(respuesta, 400, "El campo Código del artículo es obligatorio");
        assertThat(contarMovimientos()).isZero();
    }

    @ParameterizedTest
    @CsvSource({
        "codigo, El campo Código del artículo es obligatorio",
        "cantidad, El campo Cantidad es obligatorio",
        "fecha, El campo Fecha es obligatorio",
        "numeroFactura, El campo Número de factura u orden de compra es obligatorio",
        "proveedor, El campo Proveedor es obligatorio",
        "costoUnitario, El campo Costo unitario es obligatorio"
    })
    void deberia_rechazar_campos_obligatorios_ausentes(String campo, String mensajeEsperado) {
        Map<String, Object> cuerpo = cuerpoValido();
        cuerpo.remove(campo);

        verificarError(enviar(cuerpo), 400, mensajeEsperado);
        assertThat(contarMovimientos()).isZero();
    }

    @Test
    void deberia_rechazar_codigo_invalido_inexistente_o_de_otro_tipo() {
        Map<String, Object> formatoInvalido = cuerpoValido();
        formatoInvalido.put("codigo", "AT-123");
        verificarError(enviar(formatoInvalido), 400, "El Código del artículo no tiene el formato XX-00000");

        Map<String, Object> inexistente = cuerpoValido();
        inexistente.put("codigo", "AT-99999");
        verificarError(enviar(inexistente), 404, "No existe un artículo con el código AT-99999");

        Map<String, Object> productoTerminado = cuerpoValido();
        productoTerminado.put("codigo", "PT-00001");
        verificarError(enviar(productoTerminado), 400, "El artículo PT-00001 no es un artículo de terceros");

        Map<String, Object> materiaPrima = cuerpoValido();
        materiaPrima.put("codigo", "MP-00001");
        verificarError(enviar(materiaPrima), 400, "El artículo MP-00001 no es un artículo de terceros");

        assertThat(contarMovimientos()).isZero();
    }

    @Test
    void deberia_rechazar_cantidad_invalida() {
        Map<String, Object> noNumerica = cuerpoValido();
        noNumerica.put("cantidad", "cinco");
        verificarError(enviar(noNumerica), 400, "La Cantidad debe ser un valor numérico");

        Map<String, Object> decimal = cuerpoValido();
        decimal.put("cantidad", 2.5);
        verificarError(enviar(decimal), 400, "La Cantidad debe ser un número entero");

        Map<String, Object> cero = cuerpoValido();
        cero.put("cantidad", 0);
        verificarError(enviar(cero), 400, "La Cantidad debe ser mayor o igual a 1");

        assertThat(contarMovimientos()).isZero();
    }

    @Test
    void deberia_rechazar_fecha_invalida_futura_o_antigua() {
        Map<String, Object> formatoInvalido = cuerpoValido();
        formatoInvalido.put("fecha", "31/02/2026");
        verificarError(enviar(formatoInvalido), 400, "La Fecha no tiene el formato DD/MM/AAAA");

        Map<String, Object> futura = cuerpoValido();
        futura.put("fecha", hoy.plusDays(1).format(FORMATO_FECHA));
        verificarError(enviar(futura), 400, "La Fecha no puede ser posterior a la fecha actual");

        Map<String, Object> antigua = cuerpoValido();
        antigua.put("fecha", hoy.minusDays(31).format(FORMATO_FECHA));
        verificarError(enviar(antigua), 400, "La Fecha no puede tener más de 30 días de antigüedad");

        assertThat(contarMovimientos()).isZero();
    }

    @Test
    void deberia_rechazar_factura_o_proveedor_que_superen_el_maximo_de_caracteres() {
        Map<String, Object> facturaLarga = cuerpoValido();
        facturaLarga.put("numeroFactura", "F".repeat(31));
        verificarError(enviar(facturaLarga), 400,
                "El campo Número de factura u orden de compra no puede superar 30 caracteres");

        Map<String, Object> proveedorLargo = cuerpoValido();
        proveedorLargo.put("proveedor", "P".repeat(101));
        verificarError(enviar(proveedorLargo), 400,
                "El campo Proveedor no puede superar 100 caracteres");

        assertThat(contarMovimientos()).isZero();
    }

    @Test
    void deberia_rechazar_costo_unitario_invalido() {
        Map<String, Object> noNumerico = cuerpoValido();
        noNumerico.put("costoUnitario", "caro");
        verificarError(enviar(noNumerico), 400, "El Costo unitario debe ser un valor numérico");

        Map<String, Object> cero = cuerpoValido();
        cero.put("costoUnitario", 0);
        verificarError(enviar(cero), 400, "El Costo unitario debe ser mayor que 0");

        Map<String, Object> tresDecimales = cuerpoValido();
        tresDecimales.put("costoUnitario", 1500.123);
        verificarError(enviar(tresDecimales), 400, "El Costo unitario admite máximo 2 decimales");

        assertThat(contarMovimientos()).isZero();
    }

    @Test
    void deberia_migrar_desde_v5_a_v6_conservando_filas_y_validando_costo_positivo() {
        var migrador = Flyway.configure().dataSource(jdbc.getDataSource()).schemas("compra_v5");
        migrador.target(MigrationVersion.fromVersion("5")).load().migrate();

        jdbc.update("""
                INSERT INTO compra_v5.movimiento
                    (articulo_id, tipo, origen, cantidad, fecha, referencia, solicitud_id, fecha_registro)
                SELECT id, 'Ingreso', 'Fabricación', 2, CURRENT_DATE, 'LEGADO-V5', ?, CURRENT_TIMESTAMP
                FROM compra_v5.articulo WHERE codigo = 'PT-00001'
                """, UUID.randomUUID());

        var flyway = migrador.target(MigrationVersion.fromVersion("6")).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("6");

        Map<String, Object> legado = jdbc.queryForMap("""
            SELECT referencia, proveedor, costo_unitario 
            FROM compra_v5.movimiento WHERE referencia = 'LEGADO-V5'
            """);

        assertThat(legado.get("proveedor")).isNull();
        assertThat(legado.get("costo_unitario")).isNull();

        assertThatThrownBy(() -> jdbc.update("""
                UPDATE compra_v5.movimiento SET costo_unitario = 0 WHERE referencia = 'LEGADO-V5'
                """)).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_movimiento_costo_unitario");
    }

    private Map<String, Object> cuerpoValido() {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("codigo", "AT-91001");
        cuerpo.put("cantidad", 5);
        cuerpo.put("fecha", hoy.format(FORMATO_FECHA));
        cuerpo.put("numeroFactura", "FAC-2026-001");
        cuerpo.put("proveedor", "Distribuidora Industrial S.A.");
        cuerpo.put("costoUnitario", new BigDecimal("1250000.50"));
        return cuerpo;
    }

    private RespuestaHttp enviar(Map<String, Object> cuerpo) {
        return enviarCrudo(json.writeValueAsString(cuerpo));
    }

    private RespuestaHttp enviarCrudo(String cuerpoJson) {
        RestClient cliente = RestClient.builder().baseUrl("http://localhost:" + puerto).build();
        var peticion = cliente.post().uri(RUTA);
        if (cuerpoJson != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).body(cuerpoJson);
        }
        return peticion.exchange((req, res) -> new RespuestaHttp(
                res.getStatusCode().value(),
                new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private void verificarError(RespuestaHttp respuesta, int estadoEsperado, String mensajeEsperado) {
        assertThat(respuesta.estado()).isEqualTo(estadoEsperado);
        ErrorResponse error = json.readValue(respuesta.cuerpo(), ErrorResponse.class);
        assertThat(error.codigo()).isEqualTo(estadoEsperado);
        assertThat(error.mensaje()).isEqualTo(mensajeEsperado);
    }

    private BigDecimal existenciasDe(String codigo) {
        return jdbc.queryForObject("SELECT existencias FROM articulo WHERE codigo = ?", BigDecimal.class, codigo);
    }

    private int contarMovimientos() {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM movimiento", Integer.class);
        return total == null ? 0 : total;
    }

    private record RespuestaHttp(int estado, String cuerpo) {
    }
}
