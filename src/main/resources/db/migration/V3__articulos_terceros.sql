-- HU_CUTIT_14: registrar artículo de terceros.

-- La HU permite una descripción opcional de hasta 500 caracteres.
ALTER TABLE articulo ADD COLUMN descripcion VARCHAR(500);

-- DAS: tabla Consecutivo. Guarda el último número usado de cada prefijo (AT, PT, MP).
CREATE TABLE consecutivo (
    prefijo       VARCHAR(2) PRIMARY KEY,
    ultimo_valor  INTEGER    NOT NULL DEFAULT 0
);

-- Un consecutivo por prefijo, empezando donde quedaron los datos de demostración (V2):
-- PT-00002 y MP-00001 ya existen; el primer artículo de terceros será AT-00001.
INSERT INTO consecutivo (prefijo, ultimo_valor) VALUES ('PT', 2), ('MP', 1), ('AT', 0);

-- Nombre único sin importar mayúsculas (criterio 6). Es la última barrera si dos personas
-- registran el mismo nombre al mismo tiempo.
CREATE UNIQUE INDEX uk_articulo_nombre_lower ON articulo (lower(nombre));