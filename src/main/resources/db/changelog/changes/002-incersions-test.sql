--liquibase formatted sql
--changeset equipo:002-incersions-test.sql
--comment: Registros para desarrollo

-- ============================================================
-- DATOS INICIALES (SEED)
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
                              ('Dell'), ('HP'), ('Lenovo'), ('Apple'), ('Epson'),
                              ('LG'), ('Samsung'), ('Logitech'), ('Sony'), ('Cisco');

INSERT INTO locations (name, building, campus, description) VALUES
                                                                ('Laboratorio de Cómputo A', 'Edificio L',              'LOMA_BONITA', 'Planta Alta, Aula 5'),
                                                                ('Oficina de Zootecnia',     'Edificio Administrativo', 'LOMA_BONITA', 'Cubículo 3'),
                                                                ('Almacén General',          'Bodega Central',          'LOMA_BONITA', 'Área de recepción de bienes');

-- Guardians primero — los usuarios se vinculan a ellos
INSERT INTO guardians (employee_number, full_name, email, department, location_id) VALUES
                                                                                       ('EMP-001', 'Karen Jimenez Rendon',  'karen.jimenez@unpa.edu.mx', 'Administración',       2),
                                                                                       ('EMP-002', 'Juan Pérez',            'operador@unpa.edu.mx',      'Soporte Técnico',      1),
                                                                                       ('DOC-100', 'M.E. Yesenia Barrientos Arenal', 'ybarrientos@unpa.edu.mx', 'Zootecnia',    2),
                                                                                       ('DOC-101', 'Dr. Roberto García',    'rgarcia@unpa.edu.mx',       'Sistemas Automotrices', 1),
                                                                                       ('DOC-102', 'M.C. Laura Torres',     'ltorres@unpa.edu.mx',       'Investigación',        3);

-- Usuarios vinculados a sus guardians (sin email, full_name ni employee_number en users)
-- guardian_id 1 = Karen (ADMIN), guardian_id 2 = Juan (OPERADOR)
-- Hash: $2a$10$cGgoScHbjEMsVTbcVYd/Vuja8kG2WDFp980EXiaDR1XNtLUE0EKn. = EMP-001
-- Hash: $2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16  = EMP-002
INSERT INTO users (username, password_hash, role, guardian_id) VALUES
                                                                   ('admin_karen', '$2a$10$cGgoScHbjEMsVTbcVYd/Vuja8kG2WDFp980EXiaDR1XNtLUE0EKn.', 'ADMIN',    1),
                                                                   ('operador1',   '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 2);

INSERT INTO suppliers (name, rfc, created_at, updated_at) VALUES
                                                              ('Dell México S.A. de C.V.', 'DME920101XX1', NOW(), NOW()),
                                                              ('Office Depot',             'ODE930215XX2', NOW(), NOW());

INSERT INTO invoices (invoice_number, supplier_id, invoice_date, total_amount, created_by) VALUES
                                                                                               ('FACT-2026-A1', 1, '2026-01-15', 45000.00, 1),
                                                                                               ('FACT-2026-B2', 2, '2026-02-10', 12500.50, 1);

INSERT INTO assets (inventory_number, description, brand_id, model, serial_number, category_id, location_id, invoice_id, entry_date, lifecycle_status, condition_status, created_by, updated_by) VALUES
                                                                                                                                                                                                     ('INV-2026-00001', 'Laptop Dell Latitude 5420',  1,    'Latitude 5420', 'SN-DELL-001', 8, 1, 1, '2026-01-20', 'ASSIGNED',       'GOOD',    1, 1),
                                                                                                                                                                                                     ('INV-2026-00002', 'Monitor LG 24 Pulgadas',     6,    '24MK430H',      'SN-LG-992',   7, 1, 1, '2026-01-20', 'AVAILABLE',      'GOOD',    1, 1),
                                                                                                                                                                                                     ('INV-2026-00003', 'Escritorio Ejecutivo Madera', NULL, 'Mod-2024',     NULL,          1, 2, 2, '2026-02-15', 'ASSIGNED',       'REGULAR', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00004', 'Proyector Epson',             5,    'PowerLite',     'SN-EPS-332',  7, 3, 2, '2026-02-15', 'IN_MAINTENANCE', 'BAD',     1, 1);

INSERT INTO asset_assignments (asset_id, guardian_id, location_id, assigned_by, notes) VALUES
                                                                                           (1, 3, 1, 1, 'Equipo entregado para labores docentes'),
                                                                                           (3, 3, 2, 1, 'Mobiliario de oficina asignado');

INSERT INTO incidents (asset_id, description, status, condition_at_incident, created_by) VALUES
    (4, 'El lente del proyector muestra una mancha amarilla constante.', 'OPEN', 'BAD', 2);

INSERT INTO maintenance_logs (asset_id, maintenance_type, description, performed_by, performed_date, cost, condition_before, condition_after, created_by) VALUES
    (1, 'PREVENTIVE', 'Limpieza interna y cambio de pasta térmica', 'Soporte Técnico UNPA', '2026-03-01', 0.00, 'GOOD', 'GOOD', 1);