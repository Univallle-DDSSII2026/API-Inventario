package co.edu.univalle.inventario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * API de Inventario (Equipo 4). {@code @EnableAsync}: la notificación de stock bajo se envía después del commit,
 * sin hacer esperar al cliente (DAS v1.3, sección 4).
 */
@SpringBootApplication
@EnableAsync
public class InventarioApplication {

    /**
     * Punto de entrada de la aplicación.
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(InventarioApplication.class, args);
    }
}
