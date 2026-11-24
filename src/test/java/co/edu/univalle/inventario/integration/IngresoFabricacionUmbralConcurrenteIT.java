package co.edu.univalle.inventario.integration;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.univalle.inventario.command.DefinirUmbralCommand;
import co.edu.univalle.inventario.command.RegistrarIngresoFabricacionCommand;
import co.edu.univalle.inventario.service.ArticuloService;
import co.edu.univalle.inventario.service.IngresoFabricacionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
    "logging.level.org.hibernate.SQL=DEBUG",
    "logging.level.org.hibernate.orm.jdbc.bind=TRACE"
})
@ActiveProfiles("it")
class IngresoFabricacionUmbralConcurrenteIT {

    private static final String CODIGO = "PT-91003";

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17.9-alpine"));

    static {
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transacciones;

    @Autowired
    private ArticuloService umbrales;

    @Autowired
    private IngresoFabricacionService ingresos;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM movimiento WHERE articulo_id = (SELECT id FROM articulo WHERE codigo = ?)", CODIGO);
        jdbc.update("""
                INSERT INTO articulo (codigo, nombre, tipo, existencias, umbral_minimo)
                VALUES (?, 'Producto prueba HU03 y HU06', 'PRODUCTO_TERMINADO', 10, 5)
                ON CONFLICT (codigo) DO UPDATE SET existencias = 10, umbral_minimo = 5
                """, CODIGO);
    }

    @Test
    void deberia_conservar_el_ingreso_si_hu03_bloquea_primero() throws Exception {
        comprobarEspera(this::registrarIngreso, this::actualizarUmbral);
    }

    @Test
    void deberia_conservar_el_umbral_si_hu06_bloquea_primero() throws Exception {
        comprobarEspera(this::actualizarUmbral, this::registrarIngreso);
    }

    @Test
    void deberia_conservar_el_umbral_si_hu06_confirma_antes_del_ingreso() {
        actualizarUmbral();
        registrarIngreso();
        verificarEstado();
    }

    private void comprobarEspera(Runnable primeraOperacion, Runnable segundaOperacion) throws Exception {
        var transaccion = new TransactionTemplate(transacciones);
        var primerPid = new AtomicInteger();
        var segundoPid = new AtomicInteger();
        var primeraRetieneBloqueo = new CountDownLatch(1);
        var segundaIniciada = new CountDownLatch(1);
        var permitirConfirmacion = new CountDownLatch(1);

        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var primera = ejecutor.submit(() -> transaccion.executeWithoutResult(estado -> {
                primerPid.set(pid());
                primeraOperacion.run();
                primeraRetieneBloqueo.countDown();
                esperar(permitirConfirmacion);
            }));
            try {
                esperar(primeraRetieneBloqueo);
                var segunda = ejecutor.submit(() -> transaccion.executeWithoutResult(estado -> {
                    segundoPid.set(pid());
                    segundaIniciada.countDown();
                    segundaOperacion.run();
                }));
                esperar(segundaIniciada);
                esperarBloqueo(primerPid.get(), segundoPid.get());
                assertThat(segunda.isDone()).isFalse();
                permitirConfirmacion.countDown();
                primera.get(30, TimeUnit.SECONDS);
                segunda.get(30, TimeUnit.SECONDS);
            } finally {
                permitirConfirmacion.countDown();
            }
        }

        verificarEstado();
    }

    private int pid() {
        return jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
    }

    private void esperarBloqueo(int bloqueador, int bloqueado) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        do {
            Boolean esperaConfirmada = jdbc.query("""
                    SELECT ? = ANY(pg_blocking_pids(pid))
                    FROM pg_stat_activity
                    WHERE pid = ? AND wait_event_type = 'Lock'
                      AND query ILIKE '%for no key update%'
                    """, fila -> fila.next() && fila.getBoolean(1), bloqueador, bloqueado);
            if (Boolean.TRUE.equals(esperaConfirmada)) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(25);
        } while (System.nanoTime() < limite);

        throw new AssertionError("PostgreSQL no observó la segunda búsqueda esperando el bloqueo de la primera");
    }

    private void actualizarUmbral() {
        umbrales.definirUmbral(new DefinirUmbralCommand(CODIGO, "7"));
    }

    private void registrarIngreso() {
        String fecha = LocalDate.now(ZoneId.of("America/Bogota")).format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
        assertThat(ingresos.registrar(new RegistrarIngresoFabricacionCommand(CODIGO, "3", fecha, "CONCURRENCIA")))
                .endsWith("Existencias actuales: 13");
    }

    private void verificarEstado() {
        var fila = jdbc.queryForMap("SELECT existencias, umbral_minimo FROM articulo WHERE codigo = ?", CODIGO);
        Integer movimientos = jdbc.queryForObject("""
                SELECT COUNT(*) FROM movimiento m JOIN articulo a ON m.articulo_id = a.id
                WHERE a.codigo = ? AND m.tipo = 'Ingreso' AND m.cantidad = 3
                """, Integer.class, CODIGO);
        assertThat(movimientos).as("movimientos de ingreso confirmados").isEqualTo(1);
        assertThat((BigDecimal) fila.get("umbral_minimo")).as("umbral persistido").isEqualByComparingTo("7");
        assertThat((BigDecimal) fila.get("existencias")).as("existencias persistidas")
                .isEqualByComparingTo("13");
    }

    private static void esperar(CountDownLatch evento) {
        try {
            assertThat(evento.await(20, TimeUnit.SECONDS)).as("evento de la otra transacción").isTrue();
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            throw new AssertionError("La prueba concurrente fue interrumpida", excepcion);
        }
    }
}
