package co.edu.univalle.inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.univalle.inventario.command.RegistrarIngresoFabricacionCommand;
import co.edu.univalle.inventario.dto.ErrorResponse;
import co.edu.univalle.inventario.dto.IngresoFabricacionRegistradoDto;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import co.edu.univalle.inventario.service.IngresoFabricacionService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class IngresoFabricacionIT {

    private static final String ERROR_CAPACIDAD =
            "La cantidad o las existencias resultantes exceden la capacidad de almacenamiento exacto";

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17.9-alpine"));

    static {
        POSTGRES.start();
    }

    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private IngresoFabricacionService servicio;

    @Autowired
    private ArticuloRepository articulos;

    @Autowired
    private PlatformTransactionManager transacciones;

    @Value("${local.server.port}")
    private int puerto;

    private LocalDate hoy;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM movimiento");
        jdbc.update("UPDATE articulo SET existencias = 10 WHERE codigo = 'PT-00001'");
        jdbc.update("""
                INSERT INTO articulo (codigo, nombre, tipo)
                VALUES ('AT-90001', 'Repuesto HU03', 'ARTICULO_TERCEROS')
                ON CONFLICT (codigo) DO NOTHING
                """);
        hoy = LocalDate.now(ZoneId.of("America/Bogota"));
    }

    @Test
    void deberia_persistir_movimientos_repetidos_y_el_saldo() {
        Instant antes = Instant.now().minusSeconds(1);
        String cuerpo = ingreso("PT-00001", "3");
        Respuesta primera = enviar(cuerpo);
        Respuesta segunda = enviar(cuerpo);

        assertThat(primera.estado()).isEqualTo(201);
        var confirmacion = json.readValue(primera.cuerpo(), IngresoFabricacionRegistradoDto.class);
        assertThat(confirmacion.mensaje()).isEqualTo(
                "Ingreso registrado: 3 de PT-00001 – Cadena Ripit 5000. Existencias actuales: 13");
        assertThat(segunda.estado()).isEqualTo(201);
        assertThat(saldo()).isEqualByComparingTo("16");
        assertThat(contarMovimientos()).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT solicitud_id FROM movimiento", UUID.class))
                .doesNotContainNull().doesNotHaveDuplicates().hasSize(2);
        assertThat(jdbc.queryForList("""
                SELECT a.codigo, m.tipo, m.origen, m.cantidad, m.fecha, m.referencia
                FROM movimiento m JOIN articulo a ON a.id = m.articulo_id
                """)).allSatisfy(fila -> {
                    assertThat(fila).containsEntry("codigo", "PT-00001").containsEntry("tipo", "Ingreso")
                            .containsEntry("origen", "Fabricación").containsEntry("referencia", " LOTE-42 ");
                    assertThat((BigDecimal) fila.get("cantidad")).isEqualByComparingTo("3");
                    assertThat(fila.get("fecha")).isEqualTo(java.sql.Date.valueOf(hoy));
                });
        assertThat(jdbc.query("SELECT fecha_registro FROM movimiento",
                (fila, numero) -> fila.getObject(1, OffsetDateTime.class).toInstant()))
                .allSatisfy(fecha -> assertThat(fecha).isBetween(antes, Instant.now().plusSeconds(1)));
    }

    @ParameterizedTest
    @CsvSource({
        "MP-00001, 400, El artículo MP-00001 no es un producto terminado",
        "AT-90001, 400, El artículo AT-90001 no es un producto terminado",
        "PT-99999, 404, No existe un artículo con el código PT-99999",
        "XX-00001, 400, El Código del artículo no tiene el formato XX-00000"
    })
    void deberia_rechazar_articulos_invalidos(String codigo, int estado, String mensaje) {
        Respuesta respuesta = enviar(ingreso(codigo, "2"));
        assertThat(respuesta.estado()).isEqualTo(estado);
        assertThat(json.readValue(respuesta.cuerpo(), ErrorResponse.class).mensaje()).isEqualTo(mensaje);
        assertThat(contarMovimientos()).isZero();
    }

    @Test
    void deberia_distinguir_campos_ausentes_nulos_y_tipos_json() {
        verificarError("{}", "El campo Código del artículo es obligatorio");
        verificarError("{\"codigo\":\"PT-00001\"}", "El campo Cantidad es obligatorio");
        verificarError(ingreso("PT-00001", "null"), "El campo Cantidad es obligatorio");
        verificarError(ingreso("PT-00001", "{}"), "La Cantidad debe ser un valor numérico");
        verificarError(ingreso("PT-00001", "1.00000000000000000001"), "La Cantidad debe ser un número entero");
        verificarError("{\"codigo\":\"PT-00001\",\"cantidad\":2}", "El campo Fecha es obligatorio");
        verificarError(ingreso("PT-00001", "2").replace("\" LOTE-42 \"", "null"),
                "El campo Referencia de fabricación es obligatorio");
        verificarError(ingreso("PT-00001", "\"abc\""), "La Cantidad debe ser un valor numérico");
        verificarError(ingreso("PT-00001", "0"), "La Cantidad debe ser mayor o igual a 1");
        verificarError(ingreso("PT-00001", "-1"), "La Cantidad debe ser mayor o igual a 1");
        verificarError(ingreso("PT-00001", "2").replace("\" LOTE-42 \"", "\"" + "a".repeat(31) + "\""),
                "El campo Referencia de fabricación no puede superar 30 caracteres");
        verificarError(conFecha("31/02/2024"), "La Fecha no tiene el formato DD/MM/AAAA");
        verificarError(conFecha(formatear(hoy.plusDays(2))), "La Fecha no puede ser posterior a la fecha actual");
        verificarError(conFecha(formatear(hoy.minusDays(31))), "La Fecha no puede tener más de 30 días de antigüedad");
        verificarError("{", "El cuerpo de la solicitud no es válido");
        assertThat(contarMovimientos()).isZero();
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1, 11",
        "2.000, 2, 12",
        "3e2, 300, 310",
        "1000000000000, 1000000000000, 1000000000010",
        "9223372036854775809, 9223372036854775809, 9223372036854775819",
        "999999999999, 999999999999, 1000000000009"
    })
    void deberia_guardar_cantidad_y_saldo_sin_el_limite_anterior(
            String cantidadJson, String cantidad, String saldoEsperado) {
        Respuesta respuesta = enviar(ingreso("PT-00001", cantidadJson));

        assertThat(respuesta.estado()).isEqualTo(201);
        assertThat(json.readValue(respuesta.cuerpo(), IngresoFabricacionRegistradoDto.class).mensaje())
                .isEqualTo("Ingreso registrado: %s de PT-00001 – Cadena Ripit 5000. Existencias actuales: %s"
                        .formatted(cantidad, saldoEsperado));
        assertThat(saldo()).isEqualByComparingTo(saldoEsperado);
        assertThat(jdbc.queryForObject("SELECT cantidad FROM movimiento", BigDecimal.class))
                .isEqualByComparingTo(cantidad);
    }

    @Test
    void deberia_conservar_las_fracciones_del_saldo_sin_ceros_de_relleno_en_el_mensaje() {
        jdbc.update("UPDATE articulo SET existencias = 10.2500 WHERE codigo = 'PT-00001'");

        Respuesta respuesta = enviar(ingreso("PT-00001", "1"));

        assertThat(respuesta.estado()).isEqualTo(201);
        assertThat(json.readValue(respuesta.cuerpo(), IngresoFabricacionRegistradoDto.class).mensaje())
                .isEqualTo("Ingreso registrado: 1 de PT-00001 – Cadena Ripit 5000. Existencias actuales: 11.25");
        assertThat(saldo()).isEqualByComparingTo("11.25");
    }

    @Test
    void deberia_revertir_movimiento_y_saldo_si_postgresql_rechaza_una_escritura() {
        var transaccion = new TransactionTemplate(transacciones);
        var comando = new RegistrarIngresoFabricacionCommand("PT-00001", "3", formatear(hoy), "ROLLBACK");

        assertThatThrownBy(() -> transaccion.executeWithoutResult(estado -> {
            servicio.registrar(comando);
            articulos.flush();
            assertThat(contarMovimientos()).isEqualTo(1);
            assertThat(saldo()).isEqualByComparingTo("13");

            jdbc.update("UPDATE articulo SET existencias = -1 WHERE codigo = 'PT-00001'");
        })).isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ck_articulo_existencias");

        assertThat(saldo()).isEqualByComparingTo("10");
        assertThat(contarMovimientos()).isZero();
    }

    @Test
    void deberia_rechazar_el_saldo_no_representable_que_antes_se_confirmaba_con_un_valor_incorrecto() {
        jdbc.update("UPDATE articulo SET existencias = 9e131071 WHERE codigo = 'PT-00001'");

        verificarError(ingreso("PT-00001", "9e131071"), ERROR_CAPACIDAD);
        assertThat(jdbc.queryForObject("""
                SELECT existencias = 9e131071 FROM articulo WHERE codigo = 'PT-00001'
                """, Boolean.class)).isTrue();
        assertThat(contarMovimientos()).isZero();
    }

    @Test
    void deberia_persistir_exactamente_el_mayor_orden_decimal_admitido() {
        jdbc.update("UPDATE articulo SET existencias = 0 WHERE codigo = 'PT-00001'");
        String valor = "9" + "0".repeat(131_071);

        Respuesta respuesta = enviar(ingreso("PT-00001", "9e131071"));

        assertThat(respuesta.estado()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT cantidad::text FROM movimiento", String.class)).isEqualTo(valor);
        assertThat(jdbc.queryForObject("""
                SELECT existencias::text FROM articulo WHERE codigo = 'PT-00001'
                """, String.class)).isEqualTo(valor);
        assertThat(json.readValue(respuesta.cuerpo(), IngresoFabricacionRegistradoDto.class).mensaje())
                .isEqualTo("Ingreso registrado: %s de PT-00001 – Cadena Ripit 5000. Existencias actuales: %s"
                        .formatted(valor, valor));
    }

    @ParameterizedTest
    @CsvSource({
        "1e131072, tecnico",
        "1e262144, tecnico",
        "1e1000000000, tecnico",
        "10e2147483647, tecnico",
        "1e2147483648, tecnico",
        "1e2147483649, no numerico",
        "1e-2147483648, no numerico",
        "0e1000000000, minimo",
        "1e-2147483647, fraccion"
    })
    void deberia_rechazar_exponentes_extremos_sin_escribir(String cantidad, String caso) {
        String mensaje = switch (caso) {
            case "tecnico" -> ERROR_CAPACIDAD;
            case "no numerico" -> "La Cantidad debe ser un valor numérico";
            case "minimo" -> "La Cantidad debe ser mayor o igual a 1";
            case "fraccion" -> "La Cantidad debe ser un número entero";
            default -> throw new AssertionError(caso);
        };
        verificarError(ingreso("PT-00001", cantidad), mensaje);
        assertThat(saldo()).isEqualByComparingTo("10");
        assertThat(contarMovimientos()).isZero();
    }

    @Test
    void deberia_migrar_desde_v4_conservando_las_filas_y_los_check() {
        var migrador = Flyway.configure().dataSource(jdbc.getDataSource()).schemas("capacidad_v4");
        migrador.target(MigrationVersion.fromVersion("4")).load().migrate();

        jdbc.update("""
                UPDATE capacidad_v4.articulo SET existencias = 10.25, umbral_minimo = 5.50
                WHERE codigo = 'MP-00001'
                """);
        jdbc.update("""
                INSERT INTO capacidad_v4.movimiento
                    (articulo_id, tipo, origen, cantidad, fecha, referencia, solicitud_id, fecha_registro)
                SELECT id, 'Ingreso', 'Fabricación', 1.75, CURRENT_DATE, 'LEGADO', ?, CURRENT_TIMESTAMP
                FROM capacidad_v4.articulo WHERE codigo = 'MP-00001'
                """, UUID.randomUUID());
        String consultaArticulo = "SELECT * FROM capacidad_v4.articulo WHERE codigo = 'MP-00001'";
        String consultaMovimiento = "SELECT * FROM capacidad_v4.movimiento WHERE referencia = 'LEGADO'";
        var articuloAnterior = jdbc.queryForMap(consultaArticulo);
        var movimientoAnterior = jdbc.queryForMap(consultaMovimiento);

        var flyway = migrador.target(MigrationVersion.fromVersion("5")).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("5");
        assertThat(jdbc.queryForMap(consultaArticulo)).isEqualTo(articuloAnterior);
        assertThat(jdbc.queryForMap(consultaMovimiento)).isEqualTo(movimientoAnterior);

        jdbc.update("UPDATE capacidad_v4.articulo SET existencias = 10.125 WHERE codigo = 'MP-00001'");
        jdbc.update("UPDATE capacidad_v4.movimiento SET cantidad = 1.125 WHERE referencia = 'LEGADO'");
        assertThat((BigDecimal) jdbc.queryForMap(consultaArticulo).get("existencias")).isEqualByComparingTo("10.125");
        assertThat((BigDecimal) jdbc.queryForMap(consultaMovimiento).get("cantidad")).isEqualByComparingTo("1.125");

        assertThatThrownBy(() -> jdbc.update("""
                UPDATE capacidad_v4.articulo SET existencias = -1 WHERE codigo = 'MP-00001'
                """)).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_articulo_existencias");
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE capacidad_v4.movimiento SET cantidad = 0 WHERE referencia = 'LEGADO'
                """)).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("movimiento_cantidad_check");
    }

    @Test
    void deberia_conservar_ambas_actualizaciones_concurrentes() throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var primero = ejecutor.submit(() -> {
                salida.await();
                return enviar(ingreso("PT-00001", "2"));
            });
            var segundo = ejecutor.submit(() -> {
                salida.await();
                return enviar(ingreso("PT-00001", "3"));
            });
            salida.countDown();
            assertThat(primero.get(20, TimeUnit.SECONDS).estado()).isEqualTo(201);
            assertThat(segundo.get(20, TimeUnit.SECONDS).estado()).isEqualTo(201);
        }
        assertThat(contarMovimientos()).isEqualTo(2);
        assertThat(saldo()).isEqualByComparingTo("15");
    }

    private void verificarError(String cuerpo, String mensaje) {
        Respuesta respuesta = enviar(cuerpo);
        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(json.readValue(respuesta.cuerpo(), ErrorResponse.class).mensaje()).isEqualTo(mensaje);
    }

    private BigDecimal saldo() {
        return jdbc.queryForObject("SELECT existencias FROM articulo WHERE codigo = 'PT-00001'", BigDecimal.class);
    }

    private int contarMovimientos() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM movimiento", Integer.class);
    }

    private String conFecha(String fecha) {
        return ingreso("PT-00001", "2").replace(formatear(hoy), fecha);
    }

    private String formatear(LocalDate fecha) {
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
    }

    private String ingreso(String codigo, String cantidadJson) {
        return """
                {"codigo":"%s","cantidad":%s,"fecha":"%s","referenciaFabricacion":" LOTE-42 "}
                """.formatted(codigo, cantidadJson, formatear(hoy));
    }

    private Respuesta enviar(String cuerpo) {
        return RestClient.create("http://localhost:" + puerto).post()
                .uri("/inventario/ingresos/fabricacion")
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .exchange((peticion, respuesta) -> new Respuesta(respuesta.getStatusCode().value(),
                        new String(respuesta.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private record Respuesta(int estado, String cuerpo) {
    }
}
