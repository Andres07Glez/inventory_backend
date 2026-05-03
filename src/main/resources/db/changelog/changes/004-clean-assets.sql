--liquibase formatted sql
--changeset equipo:004-clean-assets
--comment: Limpieza de tablas de activos para reestructuración

-- Desactivamos revisiones de llaves foráneas para poder vaciar tablas relacionadas
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE asset_images;
TRUNCATE TABLE asset_assignments;
TRUNCATE TABLE assets;
TRUNCATE TABLE incidents;
TRUNCATE TABLE maintenance_logs;

-- Volvemos a activar las revisiones
SET FOREIGN_KEY_CHECKS = 1;

--rollback SELECT 1;