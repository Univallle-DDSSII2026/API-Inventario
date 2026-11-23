-- HU_CUTIT_06: el Umbral mínimo de una materia prima depende de su Unidad de medida ("und" = número entero).
ALTER TABLE articulo ADD COLUMN IF NOT EXISTS unidad_medida VARCHAR(10);

-- La materia prima de demostración (V2) se cargó sin unidad: se le asigna kg.
UPDATE articulo SET unidad_medida = 'kg' WHERE tipo = 'MATERIA_PRIMA' AND unidad_medida IS NULL;