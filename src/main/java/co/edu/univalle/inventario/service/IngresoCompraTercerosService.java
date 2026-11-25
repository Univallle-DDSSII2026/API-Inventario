package co.edu.univalle.inventario.service;

import co.edu.univalle.inventario.command.RegistrarIngresoCompraTercerosCommand;
import co.edu.univalle.inventario.exception.ArticuloNoEncontradoException;
import co.edu.univalle.inventario.model.Articulo;
import co.edu.univalle.inventario.model.Movimiento;
import co.edu.univalle.inventario.repository.ArticuloRepository;
import co.edu.univalle.inventario.repository.MovimientoRepository;
import co.edu.univalle.inventario.validator.IngresoCompraTercerosValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra el movimiento de compra de artículos de terceros y actualiza las existencias en una misma transacción.
 */
@Service
@RequiredArgsConstructor
public class IngresoCompraTercerosService {

    private static final ZoneId ZONA_INVENTARIO = ZoneId.of("America/Bogota");

    private final ArticuloRepository articuloRepository;
    private final MovimientoRepository movimientoRepository;
    private final IngresoCompraTercerosValidator validator;

    /**
     * Valida y registra un ingreso por compra de artículos de terceros (HU_CUTIT_13).
     *
     * @param command datos enviados por el encargado de compras
     * @return mensaje de confirmación del ingreso con el saldo actualizado
     */
    @Transactional
    public String registrar(RegistrarIngresoCompraTercerosCommand command) {
        validator.validarObligatorios(command.codigo(), command.cantidad(), command.fecha(),
                command.numeroFactura(), command.proveedor(), command.costoUnitario());
        String codigo = validator.validarCodigo(command.codigo());
        Articulo articulo = articuloRepository.findBloqueadoByCodigo(codigo)
                .orElseThrow(() -> new ArticuloNoEncontradoException(codigo));
        validator.validarTipo(articulo);
        BigDecimal cantidad = validator.validarCantidad(command.cantidad());
        LocalDate fecha = validator.validarFecha(command.fecha(), LocalDate.now(ZONA_INVENTARIO));
        String numeroFactura = validator.validarNumeroFactura(command.numeroFactura());
        String proveedor = validator.validarProveedor(command.proveedor());
        BigDecimal costoUnitario = validator.validarCostoUnitario(command.costoUnitario());

        validator.validarRepresentabilidad(cantidad);
        validator.validarRepresentabilidad(costoUnitario);
        BigDecimal saldoResultante = articulo.getExistencias().add(cantidad);
        validator.validarRepresentabilidad(saldoResultante);

        movimientoRepository.save(Movimiento.ingresoCompra(
                articulo, cantidad, fecha, numeroFactura, proveedor, costoUnitario));
        articulo.incrementarExistencias(cantidad);
        return "Ingreso registrado: %s de %s – %s. Existencias actuales: %s".formatted(
                cantidad.toPlainString(), codigo, articulo.getNombre(), formatearSaldo(saldoResultante));
    }

    private static String formatearSaldo(BigDecimal saldo) {
        if (saldo.signum() == 0) {
            return "0";
        }
        return saldo.scale() > 0 ? saldo.stripTrailingZeros().toPlainString() : saldo.toPlainString();
    }
}
