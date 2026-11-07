-- Datos de demostración: el ejemplo no incluye el registro de artículos (HU_CUTIT_01) ni los ingresos
-- (HU_CUTIT_03), así que los artículos se cargan aquí. Quitar esta migración antes de producción.
INSERT INTO articulo (codigo, nombre, tipo, existencias, umbral_minimo) VALUES
    ('PT-00001', 'Cadena Ripit 5000', 'PRODUCTO_TERMINADO', 10, 5),
    ('PT-00002', 'Cuchilla Ripit 5000', 'PRODUCTO_TERMINADO', 3, 1),
    ('MP-00001', 'Acero templado', 'MATERIA_PRIMA', 50, 10);
