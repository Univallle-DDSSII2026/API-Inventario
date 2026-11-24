package co.edu.univalle.inventario.service;

import co.edu.univalle.inventario.command.RegistrarIngresoFabricacionCommand;
import co.edu.univalle.inventario.exception.ArticuloNoEncontradoException;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.Movimiento;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import co.edu.univalle.inventario.repository.MovimientoRepository;
import co.edu.univalle.inventario.validator.IngresoFabricacionValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra el movimiento de fabricación y actualiza el saldo en la misma transacción. */
@Service
@RequiredArgsConstructor
public class IngresoFabricacionService {

    private static final ZoneId ZONA_INVENTARIO = ZoneId.of("America/Bogota");

    private final ArticuloRepository articuloRepository;
    private final MovimientoRepository movimientoRepository;
    private final IngresoFabricacionValidator validator;

    /**
     * Valida y registra un ingreso de unidades fabricadas.
     * @param command datos recibidos del operario
     * @return mensaje de confirmación del ingreso
     */
    @Transactional
    public String registrar(RegistrarIngresoFabricacionCommand command) {
        String codigo = validator.validarCodigo(command.codigo());
        Articulo articulo = articuloRepository.findBloqueadoByCodigo(codigo)
                .orElseThrow(() -> new ArticuloNoEncontradoException(codigo));
        validator.validarTipo(articulo);
        BigDecimal cantidad = validator.validarCantidad(command.cantidad());
        LocalDate fecha = validator.validarFecha(command.fecha(), LocalDate.now(ZONA_INVENTARIO));
        String referencia = validator.validarReferencia(command.referenciaFabricacion());

        validator.validarRepresentabilidad(cantidad);
        BigDecimal saldoResultante = articulo.getExistencias().add(cantidad);
        validator.validarRepresentabilidad(saldoResultante);

        movimientoRepository.save(Movimiento.ingresoFabricacion(articulo, cantidad, fecha, referencia));
        articulo.incrementarExistencias(cantidad);
        return "Ingreso registrado: %s de %s – %s. Existencias actuales: %s".formatted(
                cantidad.toPlainString(), codigo, articulo.getNombre(),
                formatearSaldo(saldoResultante));
    }

    private String formatearSaldo(BigDecimal saldo) {
        String texto = saldo.toPlainString();
        if (saldo.scale() <= 0) {
            return texto;
        }

        int fin = texto.length();
        while (texto.charAt(fin - 1) == '0') {
            fin--;
        }
        if (texto.charAt(fin - 1) == '.') {
            fin--;
        }
        return texto.substring(0, fin);
    }
}
