--liquibase formatted sql
--changeset equipo:007-assets-relate-brands
--comment: Transformamos el campo brand de texto a una relación formal con la tabla brands

-- 1. Agregamos la columna brand_id compatible con brands.id
ALTER TABLE assets
    ADD COLUMN brand_id INT(11) NULL
    COMMENT 'FK al catálogo de marcas'
    AFTER brand;

-- 2. Creamos la restricción de llave foránea
-- Esto asegura que no se inserten marcas que no existan en el catálogo
ALTER TABLE assets
    ADD CONSTRAINT fk_assets_brand
        FOREIGN KEY (brand_id) REFERENCES brands (id)
            ON DELETE SET NULL;

-- 3. Eliminamos la columna de texto antigua
ALTER TABLE assets
DROP COLUMN brand;

--rollback ALTER TABLE assets ADD COLUMN brand VARCHAR(100) AFTER brand_id;
--rollback ALTER TABLE assets DROP FOREIGN KEY fk_assets_brand;
--rollback ALTER TABLE assets DROP COLUMN brand_id;