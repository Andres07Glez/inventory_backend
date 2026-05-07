--liquibase formatted sql
--changeset equipo:005-create-suppliers
--comment: Creacion de tabla suppliers e indice

-- ============================================================
-- SP-08: Catálogo de Proveedores
-- ============================================================

CREATE TABLE IF NOT EXISTS suppliers (
                                         id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         name         VARCHAR(200) NOT NULL UNIQUE,
                                         rfc          VARCHAR(13)  NULL UNIQUE COMMENT 'RFC del proveedor (formato mexicano)',
                                         contact_name VARCHAR(150),
                                         email        VARCHAR(150),
                                         phone        VARCHAR(25),
                                         address      VARCHAR(300),
                                         notes        TEXT,
                                         is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
                                         created_at   DATETIME     NOT NULL,
                                         updated_at   DATETIME     NOT NULL
);

-- Índice para acelerar búsquedas por nombre (Se quitó el IF NOT EXISTS)
CREATE INDEX idx_suppliers_name ON suppliers (name);