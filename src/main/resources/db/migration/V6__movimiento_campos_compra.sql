-- HU_CUTIT_13: proveedor y costo unitario para ingresos por compra de artículos de terceros.
ALTER TABLE movimiento
    ADD COLUMN proveedor VARCHAR(100),
    ADD COLUMN costo_unitario NUMERIC;

ALTER TABLE movimiento
    ADD CONSTRAINT ck_movimiento_costo_unitario CHECK (costo_unitario IS NULL OR costo_unitario > 0);
    