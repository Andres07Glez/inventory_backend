--liquibase formatted sql
--changeset equipo:006-invoices-add-supplier-fk
--comment: Relacionamos facturas con proveedores eliminando el campo de texto

-- 1. Agregamos la columna con el tipo de dato exacto de la tabla suppliers
ALTER TABLE invoices
    ADD COLUMN supplier_id BIGINT(20) NULL
  COMMENT 'FK al catálogo de proveedores'
  AFTER invoice_number;

-- 2. Creamos la relación (FK)
ALTER TABLE invoices
    ADD CONSTRAINT fk_invoices_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
            ON DELETE SET NULL;

-- 3. Borramos la columna vieja de texto
ALTER TABLE invoices
DROP COLUMN supplier;

--rollback ALTER TABLE invoices ADD COLUMN supplier VARCHAR(200) AFTER invoice_date;
--rollback ALTER TABLE invoices DROP FOREIGN KEY fk_invoices_supplier;
--rollback ALTER TABLE invoices DROP COLUMN supplier_id;