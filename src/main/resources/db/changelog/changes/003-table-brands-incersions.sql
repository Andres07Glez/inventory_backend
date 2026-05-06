--liquibase formatted sql
--changeset equipo:003-table-brands-incersions.sql
--comment: Creación de tabla brands y carga inicial de marcas

-- 1. CREACIÓN DE LA TABLA
CREATE TABLE brands (
                        id         INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        name       VARCHAR(100) NOT NULL UNIQUE,
                        is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
                        created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. INSERCIÓN DE 10 MARCAS (Tecnología y Mobiliario)
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