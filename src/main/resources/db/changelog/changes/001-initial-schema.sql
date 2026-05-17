--liquibase formatted sql
--changeset equipo:001-initial-schema
--comment: Estructura completa del sistema de inventario patrimonial


-- ============================================================
-- MÓDULO 1: USUARIOS
-- ============================================================

CREATE TABLE users (
                       id              BIGINT UNSIGNED       NOT NULL AUTO_INCREMENT,
                       username        VARCHAR(50)           NOT NULL,
                       email           VARCHAR(150)          NOT NULL,
                       password_hash   VARCHAR(255)          NOT NULL  COMMENT 'BCrypt hash — nunca texto plano',
                       full_name       VARCHAR(150)          NOT NULL,
                       employee_number VARCHAR(30)           NULL      COMMENT 'Número de empleado institucional',
                       role            ENUM('ADMIN', 'USER') NOT NULL  DEFAULT 'USER',
                       is_active       BOOLEAN               NOT NULL  DEFAULT TRUE,
                       last_login_at   TIMESTAMP             NULL,
                       created_at      TIMESTAMP             NOT NULL  DEFAULT CURRENT_TIMESTAMP,
                       updated_at      TIMESTAMP             NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       PRIMARY KEY (id),
                       UNIQUE KEY uq_users_username (username),
                       UNIQUE KEY uq_users_email    (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Usuarios del sistema. Roles: ADMIN acceso total, USER operación diaria.';

-- ============================================================
-- MÓDULO 2: CATÁLOGOS
-- ============================================================

CREATE TABLE categories (
                            id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
                            name        VARCHAR(100) NOT NULL,
                            description VARCHAR(255) NULL,
                            parent_id   INT UNSIGNED NULL      COMMENT 'Permite subcategorías (máximo 2 niveles recomendado)',
                            is_active   BOOLEAN      NOT NULL  DEFAULT TRUE,
                            created_at  TIMESTAMP    NOT NULL  DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            UNIQUE KEY uq_categories_name (name),
                            INDEX idx_categories_parent_id (parent_id),
                            CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Categorías de bienes: Mobiliario, Equipo de Cómputo, Licencias, etc.';

-- ------------------------------------------------------------
CREATE TABLE locations (
                           id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
                           name        VARCHAR(150) NOT NULL  COMMENT 'Nombre del área o aula',
                           building    VARCHAR(100) NULL      COMMENT 'Edificio o bloque',
                           campus      VARCHAR(100) NULL      COMMENT 'Campus (Loma Bonita, Tuxtepec, etc.)',
                           description VARCHAR(255) NULL,
                           is_active   BOOLEAN      NOT NULL  DEFAULT TRUE,
                           created_at  TIMESTAMP    NOT NULL  DEFAULT CURRENT_TIMESTAMP,
                           PRIMARY KEY (id),
                           INDEX idx_locations_campus (campus)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Ubicaciones físicas: campus, edificio, área o laboratorio.';

-- ------------------------------------------------------------
CREATE TABLE guardians (
                           id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                           employee_number VARCHAR(30)     NULL      COMMENT 'Número de empleado institucional',
                           full_name       VARCHAR(150)    NOT NULL,
                           email           VARCHAR(150)    NULL,
                           phone           VARCHAR(25)     NULL,
                           department      VARCHAR(150)    NULL      COMMENT 'Área o departamento',
                           location_id     INT UNSIGNED    NULL      COMMENT 'Ubicación base del resguardante. Los bienes asignados heredan esta ubicación.',
                           is_active       BOOLEAN         NOT NULL  DEFAULT TRUE,
                           created_at      TIMESTAMP       NOT NULL  DEFAULT CURRENT_TIMESTAMP,
                           updated_at      TIMESTAMP       NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           PRIMARY KEY (id),
                           UNIQUE KEY uq_guardians_employee_number (employee_number),
                           INDEX idx_guardians_full_name   (full_name),
                           INDEX idx_guardians_location_id (location_id),
                           CONSTRAINT fk_guardians_location FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Catálogo de resguardantes: personas responsables de los bienes.';

-- ------------------------------------------------------------
CREATE TABLE brands (
                        id         INT          NOT NULL AUTO_INCREMENT,
                        name       VARCHAR(100) NOT NULL,
                        is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
                        created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (id),
                        UNIQUE KEY uq_brands_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Catálogo de marcas de bienes patrimoniales.';

-- ------------------------------------------------------------
CREATE TABLE suppliers (
                           id           BIGINT       NOT NULL AUTO_INCREMENT,
                           name         VARCHAR(200) NOT NULL,
                           rfc          VARCHAR(13)  NULL     COMMENT 'RFC del proveedor (formato mexicano)',
                           contact_name VARCHAR(150) NULL,
                           email        VARCHAR(150) NULL,
                           phone        VARCHAR(25)  NULL,
                           address      VARCHAR(300) NULL,
                           notes        TEXT         NULL,
                           is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
                           created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           PRIMARY KEY (id),
                           UNIQUE KEY uq_suppliers_name (name),
                           UNIQUE KEY uq_suppliers_rfc  (rfc),
                           INDEX idx_suppliers_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Catálogo de proveedores que suministran bienes a la institución.';

-- ------------------------------------------------------------
CREATE TABLE invoices (
                          id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                          invoice_number VARCHAR(100)    NOT NULL  COMMENT 'Número de factura del proveedor',
                          supplier_id    BIGINT          NULL      COMMENT 'FK al catálogo de proveedores',
                          invoice_date   DATE            NOT NULL  COMMENT 'Fecha impresa en la factura',
                          total_amount   DECIMAL(12, 2)  NULL,
                          document_path  VARCHAR(500)    NULL      COMMENT 'Ruta al PDF/imagen de la factura digitalizada',
                          notes          TEXT            NULL,
                          created_at     TIMESTAMP       NOT NULL  DEFAULT CURRENT_TIMESTAMP,
                          created_by     BIGINT UNSIGNED NOT NULL,
                          PRIMARY KEY (id),
                          UNIQUE KEY uq_invoices_number (invoice_number),
                          CONSTRAINT fk_invoices_supplier   FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
                          CONSTRAINT fk_invoices_created_by FOREIGN KEY (created_by)  REFERENCES users(id)     ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Facturas de compra que respaldan el ingreso de bienes al inventario.';

-- ============================================================
-- MÓDULO 3: BIENES PATRIMONIALES (núcleo del sistema)
-- ============================================================

CREATE TABLE assets (
                        id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    -- Identificadores
                        inventory_number VARCHAR(30)     NOT NULL  COMMENT 'Generado por el sistema: INV-2026-00001',
                        barcode          VARCHAR(100)    NULL      COMMENT 'Código de barras institucional para escaneo',

    -- Descripción
                        description      VARCHAR(500)    NOT NULL,
                        brand_id         INT             NULL      COMMENT 'FK al catálogo de marcas',
                        model            VARCHAR(150)    NULL,
                        serial_number    VARCHAR(200)    NULL      COMMENT 'Aplica para equipo de cómputo y similares',
                        notes            TEXT            NULL,

    -- Relaciones
                        category_id      INT UNSIGNED    NOT NULL,
                        location_id      INT UNSIGNED    NULL      COMMENT 'NULL si aún no tiene ubicación asignada',
                        invoice_id       BIGINT UNSIGNED NULL      COMMENT 'Factura que sustenta el bien',

    -- Fechas
                        invoice_date     DATE            NULL      COMMENT 'Fecha de la factura (puede diferir de entry_date)',
                        entry_date       DATE            NOT NULL  COMMENT 'Fecha de entrada física al almacén',

    -- Estado (dos dimensiones independientes)
                        condition_status ENUM('GOOD', 'REGULAR', 'BAD') NOT NULL DEFAULT 'GOOD'
        COMMENT 'Condición física: GOOD=Bueno, REGULAR=Regular, BAD=Malo',

                        lifecycle_status ENUM(
        'REGISTERED',
        'AVAILABLE',
        'ASSIGNED',
        'IN_MAINTENANCE',
        'IN_WARRANTY',
        'DECOMMISSIONED'
    ) NOT NULL DEFAULT 'REGISTERED'
        COMMENT 'Ciclo de vida: REGISTERED, AVAILABLE, ASSIGNED, IN_MAINTENANCE, IN_WARRANTY, DECOMMISSIONED',

    -- Auditoría
                        created_at       TIMESTAMP       NOT NULL  DEFAULT CURRENT_TIMESTAMP,
                        updated_at       TIMESTAMP       NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        created_by       BIGINT UNSIGNED NOT NULL,
                        updated_by       BIGINT UNSIGNED NOT NULL,

                        PRIMARY KEY (id),
                        UNIQUE KEY uq_assets_inventory_number (inventory_number),
                        UNIQUE KEY uq_assets_barcode          (barcode),
                        INDEX idx_assets_serial_number    (serial_number),
                        INDEX idx_assets_category_id      (category_id),
                        INDEX idx_assets_location_id      (location_id),
                        INDEX idx_assets_brand_id         (brand_id),
                        INDEX idx_assets_lifecycle_status (lifecycle_status),
                        INDEX idx_assets_condition_status (condition_status),
                        INDEX idx_assets_entry_date       (entry_date),

                        CONSTRAINT fk_assets_brand      FOREIGN KEY (brand_id)    REFERENCES brands(id)     ON DELETE SET NULL,
                        CONSTRAINT fk_assets_category   FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
                        CONSTRAINT fk_assets_location   FOREIGN KEY (location_id) REFERENCES locations(id)  ON DELETE SET NULL,
                        CONSTRAINT fk_assets_invoice    FOREIGN KEY (invoice_id)  REFERENCES invoices(id)   ON DELETE SET NULL,
                        CONSTRAINT fk_assets_created_by FOREIGN KEY (created_by)  REFERENCES users(id)      ON DELETE RESTRICT,
                        CONSTRAINT fk_assets_updated_by FOREIGN KEY (updated_by)  REFERENCES users(id)      ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Bienes patrimoniales. El resguardante actual se obtiene desde asset_assignments.';

-- ------------------------------------------------------------
CREATE TABLE asset_images (
                              id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                              asset_id    BIGINT UNSIGNED NOT NULL,
                              file_path   VARCHAR(500)    NOT NULL  COMMENT 'Ruta relativa al archivo (file system o bucket)',
                              file_name   VARCHAR(255)    NOT NULL  COMMENT 'Nombre original del archivo',
                              mime_type   VARCHAR(100)    NOT NULL  DEFAULT 'image/jpeg',
                              is_primary  BOOLEAN         NOT NULL  DEFAULT FALSE COMMENT 'Imagen principal del bien en listados',
                              uploaded_at TIMESTAMP       NOT NULL  DEFAULT CURRENT_TIMESTAMP,
                              uploaded_by BIGINT UNSIGNED NOT NULL,
                              PRIMARY KEY (id),
                              INDEX idx_asset_images_asset_id (asset_id),
                              CONSTRAINT fk_asset_images_asset       FOREIGN KEY (asset_id)    REFERENCES assets(id) ON DELETE CASCADE,
                              CONSTRAINT fk_asset_images_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id)  ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Evidencias fotográficas de los bienes. Solo se guarda la ruta, nunca el binario.';

-- ============================================================
-- MÓDULO 4: ASIGNACIONES
-- Fuente de verdad del resguardante actual:
--   SELECT * FROM asset_assignments WHERE asset_id = ? AND returned_at IS NULL
-- ============================================================

CREATE TABLE asset_assignments (
                                   id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                   asset_id    BIGINT UNSIGNED NOT NULL,
                                   guardian_id BIGINT UNSIGNED NOT NULL,
                                   location_id INT UNSIGNED    NULL      COMMENT 'Ubicación al momento de la asignación',
                                   assigned_at TIMESTAMP       NOT NULL  DEFAULT CURRENT_TIMESTAMP,
                                   returned_at TIMESTAMP       NULL      COMMENT 'NULL = asignación activa',
                                   assigned_by BIGINT UNSIGNED NOT NULL,
                                   returned_by BIGINT UNSIGNED NULL,
                                   notes       VARCHAR(500)    NULL,
                                   PRIMARY KEY (id),
                                   INDEX idx_asset_assignments_asset_id    (asset_id),
                                   INDEX idx_asset_assignments_guardian_id (guardian_id),
                                   INDEX idx_asset_assignments_returned_at (returned_at),
                                   CONSTRAINT fk_aa_asset       FOREIGN KEY (asset_id)    REFERENCES assets(id)    ON DELETE RESTRICT,
                                   CONSTRAINT fk_aa_guardian    FOREIGN KEY (guardian_id) REFERENCES guardians(id) ON DELETE RESTRICT,
                                   CONSTRAINT fk_aa_location    FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL,
                                   CONSTRAINT fk_aa_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id)     ON DELETE RESTRICT,
                                   CONSTRAINT fk_aa_returned_by FOREIGN KEY (returned_by) REFERENCES users(id)     ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Historial de asignaciones. La fila con returned_at IS NULL es la asignación vigente.';

-- ============================================================
-- MÓDULO 5: INCIDENCIAS
-- ============================================================

CREATE TABLE incidents (
                           id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                           asset_id              BIGINT UNSIGNED NOT NULL,
                           description           TEXT            NOT NULL  COMMENT 'Descripción del problema o falla',
                           repair_type           ENUM('INTERNAL', 'EXTERNAL') NULL
        COMMENT 'INTERNAL=Reparación interna, EXTERNAL=Proveedor externo',
                           status                ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') NOT NULL DEFAULT 'OPEN',
                           condition_at_incident ENUM('GOOD', 'REGULAR', 'BAD') NOT NULL
        COMMENT 'Snapshot de la condición física al momento de reportar',
                           resolution_notes      TEXT            NULL,
                           resolved_at           TIMESTAMP       NULL,
                           resolved_by           BIGINT UNSIGNED NULL,
                           created_at            TIMESTAMP       NOT NULL  DEFAULT CURRENT_TIMESTAMP,
                           updated_at            TIMESTAMP       NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           created_by            BIGINT UNSIGNED NOT NULL,
                           PRIMARY KEY (id),
                           INDEX idx_incidents_asset_id (asset_id),
                           INDEX idx_incidents_status   (status),
                           CONSTRAINT fk_incidents_asset       FOREIGN KEY (asset_id)    REFERENCES assets(id) ON DELETE RESTRICT,
                           CONSTRAINT fk_incidents_created_by  FOREIGN KEY (created_by)  REFERENCES users(id)  ON DELETE RESTRICT,
                           CONSTRAINT fk_incidents_resolved_by FOREIGN KEY (resolved_by) REFERENCES users(id)  ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Incidencias reportadas (fallas o daños). El folio se formatea en el backend.';

-- Nota: el folio legible (INC-2026-00001) se construye en Spring Boot así:
--   String folio = "INC-" + Year.now() + "-" + String.format("%05d", incident.getId());

-- ------------------------------------------------------------
CREATE TABLE incident_images (
                                 id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                 incident_id BIGINT UNSIGNED NOT NULL,
                                 file_path   VARCHAR(500)    NOT NULL,
                                 file_name   VARCHAR(255)    NOT NULL,
                                 mime_type   VARCHAR(100)    NOT NULL  DEFAULT 'image/jpeg',
                                 uploaded_at TIMESTAMP       NOT NULL  DEFAULT CURRENT_TIMESTAMP,
                                 uploaded_by BIGINT UNSIGNED NOT NULL,
                                 PRIMARY KEY (id),
                                 INDEX idx_incident_images_incident_id (incident_id),
                                 CONSTRAINT fk_ii_incident    FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE CASCADE,
                                 CONSTRAINT fk_ii_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id)     ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Imágenes adjuntas a incidencias (evidencia fotográfica del problema).';

-- ============================================================
-- MÓDULO 6: MANTENIMIENTO
-- Identificado como PRIMORDIAL en la entrevista con el jefe de almacén.
-- ============================================================

CREATE TABLE maintenance_logs (
                                  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                  asset_id         BIGINT UNSIGNED NOT NULL,
                                  incident_id      BIGINT UNSIGNED NULL      COMMENT 'Incidencia de origen (NULL si es mantenimiento preventivo)',
                                  maintenance_type ENUM('PREVENTIVE', 'CORRECTIVE', 'WARRANTY') NOT NULL,
                                  description      TEXT            NOT NULL  COMMENT 'Trabajo realizado',
                                  performed_by     VARCHAR(200)    NULL      COMMENT 'Técnico o empresa responsable',
                                  performed_date   DATE            NOT NULL,
                                  cost             DECIMAL(10, 2)  NULL,
                                  condition_before ENUM('GOOD', 'REGULAR', 'BAD') NULL COMMENT 'Condición antes del servicio',
                                  condition_after  ENUM('GOOD', 'REGULAR', 'BAD') NULL COMMENT 'Condición después del servicio',
                                  created_at       TIMESTAMP       NOT NULL  DEFAULT CURRENT_TIMESTAMP,
                                  created_by       BIGINT UNSIGNED NOT NULL,
                                  PRIMARY KEY (id),
                                  INDEX idx_maintenance_asset_id       (asset_id),
                                  INDEX idx_maintenance_performed_date (performed_date),
                                  CONSTRAINT fk_ml_asset      FOREIGN KEY (asset_id)    REFERENCES assets(id)    ON DELETE RESTRICT,
                                  CONSTRAINT fk_ml_incident   FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE SET NULL,
                                  CONSTRAINT fk_ml_created_by FOREIGN KEY (created_by)  REFERENCES users(id)     ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Bitácora de mantenimiento y reparaciones por bien (preventivo, correctivo, garantía).';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- DATOS SEMILLA (SEED)
-- Catálogos base requeridos para el funcionamiento del sistema.
-- ============================================================

INSERT INTO categories (name, description, parent_id) VALUES
                                                          ('Bienes Muebles',        'Mobiliario en general',                    NULL),
                                                          ('Equipo de Cómputo',     'Computadoras y componentes tecnológicos',   NULL),
                                                          ('Licencias de Software', 'Licencias físicas y electrónicas',          NULL),
                                                          ('Climatización',         'Aires acondicionados y equipo de clima',    NULL),
                                                          ('Equipo de Laboratorio', 'Instrumental y equipo especializado',       NULL),
                                                          ('CPUs y Servidores',     'Unidades centrales y servidores',           2),
                                                          ('Periféricos',           'Mouse, teclado, monitor, impresoras, etc.', 2),
                                                          ('Laptops',               'Equipos portátiles',                        2);

INSERT INTO brands (name) VALUES
                              ('Dell'),
                              ('HP'),
                              ('Lenovo'),
                              ('Apple'),
                              ('Epson'),
                              ('LG'),
                              ('Samsung'),
                              ('Logitech'),
                              ('Sony'),
                              ('Cisco');