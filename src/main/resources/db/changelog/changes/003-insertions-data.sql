--liquibase formatted sql
--changeset equipo:003-insertions-data.sql
--comment: Datos de prueba masivos para el sistema de inventario (Min 30 por tabla)

-- ============================================================
-- 1. CATEGORÍAS (Sumando 22 a las 8 existentes = 30)
-- ============================================================
INSERT INTO categories (name, description, parent_id) VALUES
                                                          ('Servidores de Rack', 'Servidores físicos en centro de datos', 2), ('Dispositivos Móviles', 'Smartphones y tablets', 2),
                                                          ('Herramientas Manuales', 'Pinzas, desarmadores, llaves', NULL), ('Herramientas Eléctricas', 'Taladros, sierras, pulidoras', NULL),
                                                          ('Vehículos Oficiales', 'Autos, camionetas y motocicletas', NULL), ('Equipo Audiovisual', 'Cámaras, micrófonos, consolas', NULL),
                                                          ('Redes y Telecomunicaciones', 'Routers, Switches, Access Points', 2), ('Mobiliario de Aula', 'Pupitres, pizarrones, escritorios', 1),
                                                          ('Sillas Ergonómicas', 'Sillas para personal administrativo', 1), ('Software de Diseño', 'Licencias CAD y Suite Adobe', 3),
                                                          ('IDE y Desarrollo', 'Licencias de entornos de desarrollo', 3), ('Sistemas Operativos', 'Licencias Windows Server, Red Hat', 3),
                                                          ('Equipo Médico', 'Material para enfermería y medicina', 5), ('Microscopios', 'Microscopía óptica y electrónica', 5),
                                                          ('Instrumentación', 'Osciloscopios, multímetros', 5), ('Almacenamiento', 'Discos duros externos, NAS, SAN', 2),
                                                          ('Seguridad Perimetral', 'Firewalls de hardware', 7), ('Cámaras de Vigilancia', 'Cámaras IP y CCTV', 7),
                                                          ('Tractores y Maquinaria', 'Maquinaria pesada para agricultura', 5), ('Proyectores Interactivos', 'Proyectores de tiro corto', NULL),
                                                          ('Pantallas Interactivas', 'Pantallas táctiles para docencia', NULL), ('Línea Blanca', 'Refrigeradores, microondas', 1);

-- ============================================================
-- 2. MARCAS (Sumando 20 a las 10 existentes = 30)
-- ============================================================
INSERT INTO brands (name) VALUES
                              ('Motorola'), ('POCO'), ('Xiaomi'), ('Asus'), ('Acer'), ('MikroTik'), ('Ubiquiti'), ('Fortinet'), ('Truper'), ('Bosch'),
                              ('Nissan'), ('Chevrolet'), ('Ford'), ('Nikon'), ('Canon'), ('Shure'), ('Yamaha'), ('Synology'), ('APC'), ('CyberPower');

-- ============================================================
-- 3. UBICACIONES (Sumando 27 a las 3 existentes = 30)
-- ============================================================
INSERT INTO locations (name, building, campus, description) VALUES
                                                                ('Centro de Datos Principal', 'Edificio SITE', 'LOMA_BONITA', 'Site de telecomunicaciones central'),
                                                                ('Laboratorio de Redes', 'Edificio K', 'LOMA_BONITA', 'Prácticas de enrutamiento y conmutación'),
                                                                ('Laboratorio de Enfermería', 'Edificio Salud', 'TUXTEPEC', 'Área de simuladores médicos'),
                                                                ('Biblioteca Central', 'Edificio B', 'LOMA_BONITA', 'Área de lectura y acervo'),
                                                                ('Aula Múltiple 1', 'Edificio A', 'LOMA_BONITA', 'Aulas de tronco común'),
                                                                ('Aula Múltiple 2', 'Edificio A', 'LOMA_BONITA', 'Aulas de tronco común'),
                                                                ('Aula Múltiple 3', 'Edificio A', 'LOMA_BONITA', 'Aulas de tronco común'),
                                                                ('Cubículo Profesores 1', 'Edificio C', 'LOMA_BONITA', 'Zootecnia y Veterinaria'),
                                                                ('Cubículo Profesores 2', 'Edificio C', 'LOMA_BONITA', 'Sistemas y Mecatrónica'),
                                                                ('Auditorio Institucional', 'Edificio Central', 'LOMA_BONITA', 'Eventos y conferencias'),
                                                                ('Sala de Juntas A', 'Edificio Administrativo', 'LOMA_BONITA', 'Reuniones de rectoría'),
                                                                ('Laboratorio Quimica 1', 'Edificio Q', 'LOMA_BONITA', 'Prácticas de química general'),
                                                                ('Laboratorio Quimica 2', 'Edificio Q', 'LOMA_BONITA', 'Prácticas de química analítica'),
                                                                ('Cafetería Universitaria', 'Servicios', 'LOMA_BONITA', 'Comedor de estudiantes'),
                                                                ('Taller Mecánico', 'Nave Industrial', 'LOMA_BONITA', 'Sistemas Automotrices'),
                                                                ('Cancha de Usos Múltiples', 'Deportivo', 'LOMA_BONITA', 'Área deportiva'),
                                                                ('Invernadero 1', 'Campo Agrícola', 'LOMA_BONITA', 'Prácticas de agronomía'),
                                                                ('Invernadero 2', 'Campo Agrícola', 'LOMA_BONITA', 'Prácticas de agronomía'),
                                                                ('Corrales de Manejo', 'Zootecnia', 'LOMA_BONITA', 'Manejo de ganado'),
                                                                ('Caseta de Vigilancia Principal', 'Entrada', 'LOMA_BONITA', 'Control de acceso'),
                                                                ('Oficina de Control Escolar', 'Edificio Administrativo', 'TUXTEPEC', 'Trámites escolares'),
                                                                ('Laboratorio Cómputo B', 'Edificio L', 'TUXTEPEC', 'Planta Baja'),
                                                                ('Laboratorio Cómputo C', 'Edificio L', 'TUXTEPEC', 'Edificio nuevo'),
                                                                ('Clínica Veterinaria', 'Zootecnia', 'LOMA_BONITA', 'Atención de pequeñas especies'),
                                                                ('Sala de Titulación', 'Edificio Administrativo', 'LOMA_BONITA', 'Exámenes profesionales'),
                                                                ('Archivo General', 'Sótano', 'LOMA_BONITA', 'Resguardo de documentos físicos'),
                                                                ('Oficina de Recursos Humanos', 'Edificio Administrativo', 'LOMA_BONITA', 'Gestión de personal');

-- ============================================================
-- 4. RESGUARDANTES (Sumando 28 a los 5 existentes = 33, suficiente para vincular usuarios)
-- Nota: los IDs 1-5 ya existen del changeset 002.
-- Los IDs aquí serán 6 en adelante.
-- ============================================================
INSERT INTO guardians (employee_number, full_name, email, department, location_id) VALUES
                                                                                       ('EMP-003', 'Carlos Ruiz',      'aud1@unpa.edu.mx',       'Auditoría',              4),
                                                                                       ('EMP-004', 'Ana Gómez',        'gua1@unpa.edu.mx',       'Docencia',               5),
                                                                                       ('EMP-005', 'Miguel Sánchez',   'miguel.op@unpa.edu.mx',  'Soporte Técnico',        4),
                                                                                       ('EMP-006', 'Luisa Méndez',     'luisa.op@unpa.edu.mx',   'Soporte Técnico',        4),
                                                                                       ('EMP-007', 'Pedro Castillo',   'pedro.adm@unpa.edu.mx',  'Administración',         4),
                                                                                       ('EMP-008', 'Usuario Ocho',     'u08@unpa.edu.mx',        'Área General',           5),
                                                                                       ('EMP-009', 'Usuario Nueve',    'u09@unpa.edu.mx',        'Área General',           5),
                                                                                       ('EMP-010', 'Usuario Diez',     'u10@unpa.edu.mx',        'Área General',           5),
                                                                                       ('EMP-011', 'Usuario Once',     'u11@unpa.edu.mx',        'Área General',           5),
                                                                                       ('EMP-012', 'Usuario Doce',     'u12@unpa.edu.mx',        'Área General',           5),
                                                                                       ('EMP-013', 'Usuario Trece',    'u13@unpa.edu.mx',        'Área General',           5),
                                                                                       ('EMP-014', 'Usuario Catorce',  'u14@unpa.edu.mx',        'Área General',           5),
                                                                                       ('EMP-015', 'Usuario Quince',   'u15@unpa.edu.mx',        'Área General',           6),
                                                                                       ('EMP-016', 'Usuario Dieciseis','u16@unpa.edu.mx',        'Área General',           6),
                                                                                       ('EMP-017', 'Usuario Diecisiete','u17@unpa.edu.mx',       'Área General',           6),
                                                                                       ('EMP-018', 'Usuario Dieciocho','u18@unpa.edu.mx',        'Área General',           6),
                                                                                       ('EMP-019', 'Usuario Diecinueve','u19@unpa.edu.mx',       'Área General',           7),
                                                                                       ('EMP-020', 'Usuario Veinte',   'u20@unpa.edu.mx',        'Área General',           7),
                                                                                       ('EMP-021', 'Usuario Veintiuno','u21@unpa.edu.mx',        'Área General',           7),
                                                                                       ('EMP-022', 'Usuario Veintidos','u22@unpa.edu.mx',        'Área General',           7),
                                                                                       ('EMP-023', 'Usuario Veintitres','u23@unpa.edu.mx',       'Área General',           8),
                                                                                       ('EMP-024', 'Usuario Veinticuatro','u24@unpa.edu.mx',     'Área General',           8),
                                                                                       ('EMP-025', 'Usuario Veinticinco','u25@unpa.edu.mx',      'Área General',           8),
                                                                                       ('EMP-026', 'Usuario Veintiseis','u26@unpa.edu.mx',       'Área General',           8),
                                                                                       ('EMP-027', 'Usuario Veintisiete','u27@unpa.edu.mx',      'Área General',           9),
                                                                                       ('EMP-028', 'Usuario Veintiocho','u28@unpa.edu.mx',       'Área General',           9),
                                                                                       ('EMP-029', 'Usuario Veintinueve','u29@unpa.edu.mx',      'Área General',           9),
                                                                                       ('EMP-030', 'Usuario Treinta',  'u30@unpa.edu.mx',        'Área General',           9);

INSERT INTO guardians (employee_number, full_name, department, location_id) VALUES
                                                                                ('DOC-103', 'Ing. Fernando Velez',    'Sistemas Computacionales', 4),
                                                                                ('DOC-104', 'Dra. Carmen Salinas',    'Enfermería',               6),
                                                                                ('DOC-105', 'M.A. Patricia Aguilar', 'Administración',           14),
                                                                                ('DOC-106', 'Lic. Eduardo Medina',   'Biblioteca',               7),
                                                                                ('DOC-107', 'Ing. Arturo Vidal',     'Soporte Técnico',          4),
                                                                                ('DOC-108', 'Biol. Sandra López',    'Química',                  15),
                                                                                ('DOC-109', 'Dr. Mario Castañeda',   'Zootecnia',                12),
                                                                                ('DOC-110', 'Ing. Sofía Reyes',      'Mecatrónica',              18),
                                                                                ('DOC-111', 'Resguardante 11', 'Área General', 5), ('DOC-112', 'Resguardante 12', 'Área General', 5),
                                                                                ('DOC-113', 'Resguardante 13', 'Área General', 5), ('DOC-114', 'Resguardante 14', 'Área General', 5),
                                                                                ('DOC-115', 'Resguardante 15', 'Área General', 6), ('DOC-116', 'Resguardante 16', 'Área General', 6),
                                                                                ('DOC-117', 'Resguardante 17', 'Área General', 6), ('DOC-118', 'Resguardante 18', 'Área General', 6),
                                                                                ('DOC-119', 'Resguardante 19', 'Área General', 7), ('DOC-120', 'Resguardante 20', 'Área General', 7),
                                                                                ('DOC-121', 'Resguardante 21', 'Área General', 7), ('DOC-122', 'Resguardante 22', 'Área General', 8),
                                                                                ('DOC-123', 'Resguardante 23', 'Área General', 8), ('DOC-124', 'Resguardante 24', 'Área General', 8),
                                                                                ('DOC-125', 'Resguardante 25', 'Área General', 9), ('DOC-126', 'Resguardante 26', 'Área General', 9),
                                                                                ('DOC-127', 'Resguardante 27', 'Área General', 9), ('DOC-128', 'Resguardante 28', 'Área General', 10),
                                                                                ('DOC-129', 'Resguardante 29', 'Área General', 10);

-- ============================================================
-- 5. USUARIOS (28 nuevos vinculados a sus guardians, solo username + password_hash + role + guardian_id)
-- El hash $2a$10$6OILz... equivale a la contraseña genérica (número de empleado)
-- guardian_id 6  = Carlos Ruiz    (EMP-003) → AUDITOR
-- guardian_id 7  = Ana Gómez      (EMP-004) → GUARDIAN
-- guardian_id 8  = Miguel Sánchez (EMP-005) → OPERADOR
-- guardian_id 9  = Luisa Méndez   (EMP-006) → OPERADOR
-- guardian_id 10 = Pedro Castillo (EMP-007) → ADMIN
-- guardian_id 11..33 = EMP-008..030 → OPERADOR / AUDITOR / GUARDIAN según seed
-- ============================================================
INSERT INTO users (username, password_hash, role, guardian_id) VALUES
                                                                   ('auditor1', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'AUDITOR',  6),
                                                                   ('guardian1','$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'GUARDIAN', 7),
                                                                   ('op_miguel','$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 8),
                                                                   ('op_luisa', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 9),
                                                                   ('adm_pedro','$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'ADMIN',    10);

INSERT INTO users (username, password_hash, role, guardian_id) VALUES
                                                                   ('user08', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 11),
                                                                   ('user09', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'AUDITOR',  12),
                                                                   ('user10', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'GUARDIAN', 13),
                                                                   ('user11', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 14),
                                                                   ('user12', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 15),
                                                                   ('user13', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 16),
                                                                   ('user14', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 17),
                                                                   ('user15', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 18),
                                                                   ('user16', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 19),
                                                                   ('user17', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 20),
                                                                   ('user18', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 21),
                                                                   ('user19', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 22),
                                                                   ('user20', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 23),
                                                                   ('user21', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 24),
                                                                   ('user22', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 25),
                                                                   ('user23', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 26),
                                                                   ('user24', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 27),
                                                                   ('user25', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 28),
                                                                   ('user26', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 29),
                                                                   ('user27', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 30),
                                                                   ('user28', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 31),
                                                                   ('user29', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 32),
                                                                   ('user30', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'OPERADOR', 33);

-- ============================================================
-- 6. PROVEEDORES (Sumando 28 a los 2 existentes = 30)
-- ============================================================
INSERT INTO suppliers (name, rfc) VALUES
                                      ('Syscom S.A. de C.V.', 'SYS980101XYZ'), ('Amazon México', 'AME140501ABC'),
                                      ('Mercado Libre', 'MLI101010QWE'), ('PC en Línea', 'PCL090909RTY'),
                                      ('Cyberpuerta', 'CYB110202UIO'), ('Steren', 'STE800505PAS'),
                                      ('Home Depot', 'HDE990101FGH'), ('OfficeMax', 'OMA920303JKL'),
                                      ('Distribuidora Médica del Sur', 'DMS880404ZXC'), ('Muebles Dico', 'MDI770707VBN'),
                                      ('Prov 11 S.A.', 'PRV000000011'), ('Prov 12 S.A.', 'PRV000000012'), ('Prov 13 S.A.', 'PRV000000013'),
                                      ('Prov 14 S.A.', 'PRV000000014'), ('Prov 15 S.A.', 'PRV000000015'), ('Prov 16 S.A.', 'PRV000000016'),
                                      ('Prov 17 S.A.', 'PRV000000017'), ('Prov 18 S.A.', 'PRV000000018'), ('Prov 19 S.A.', 'PRV000000019'),
                                      ('Prov 20 S.A.', 'PRV000000020'), ('Prov 21 S.A.', 'PRV000000021'), ('Prov 22 S.A.', 'PRV000000022'),
                                      ('Prov 23 S.A.', 'PRV000000023'), ('Prov 24 S.A.', 'PRV000000024'), ('Prov 25 S.A.', 'PRV000000025'),
                                      ('Prov 26 S.A.', 'PRV000000026'), ('Prov 27 S.A.', 'PRV000000027'), ('Prov 28 S.A.', 'PRV000000028');

-- ============================================================
-- 7. FACTURAS (30 registros nuevos)
-- ============================================================
INSERT INTO invoices (invoice_number, supplier_id, invoice_date, total_amount, created_by) VALUES
                                                                                               ('F-2025-001', 3, '2025-01-10', 150000.00, 1), ('F-2025-002', 4, '2025-02-15', 34500.20, 1),
                                                                                               ('F-2025-003', 5, '2025-03-20', 12000.00, 2),  ('F-2025-004', 6, '2025-04-05', 8900.50, 2),
                                                                                               ('F-2025-005', 7, '2025-05-12', 4500.00, 1),   ('F-2025-006', 8, '2025-06-18', 21500.00, 1),
                                                                                               ('F-2025-007', 9, '2025-07-22', 112000.00, 2), ('F-2025-008', 10, '2025-08-30', 9800.00, 2),
                                                                                               ('F-2025-009', 11, '2025-09-01', 5400.00, 1),  ('F-2025-010', 12, '2025-10-10', 13200.00, 1),
                                                                                               ('F-2025-011', 13, '2025-10-15', 5000.00, 1),  ('F-2025-012', 14, '2025-10-16', 6000.00, 1),
                                                                                               ('F-2025-013', 15, '2025-10-17', 7000.00, 1),  ('F-2025-014', 16, '2025-10-18', 8000.00, 1),
                                                                                               ('F-2025-015', 17, '2025-10-19', 9000.00, 1),  ('F-2025-016', 18, '2025-10-20', 10000.00, 1),
                                                                                               ('F-2025-017', 19, '2025-10-21', 11000.00, 1), ('F-2025-018', 20, '2025-10-22', 12000.00, 1),
                                                                                               ('F-2025-019', 21, '2025-10-23', 13000.00, 1), ('F-2025-020', 22, '2025-10-24', 14000.00, 1),
                                                                                               ('F-2025-021', 23, '2025-10-25', 15000.00, 1), ('F-2025-022', 24, '2025-10-26', 16000.00, 1),
                                                                                               ('F-2025-023', 25, '2025-10-27', 17000.00, 1), ('F-2025-024', 26, '2025-10-28', 18000.00, 1),
                                                                                               ('F-2025-025', 27, '2025-10-29', 19000.00, 1), ('F-2025-026', 28, '2025-10-30', 20000.00, 1),
                                                                                               ('F-2025-027', 29, '2025-11-01', 21000.00, 1), ('F-2025-028', 30, '2025-11-02', 22000.00, 1),
                                                                                               ('F-2025-029', 1,  '2025-11-03', 23000.00, 1), ('F-2025-030', 2,  '2025-11-04', 24000.00, 1);

-- ============================================================
-- 8. BIENES PATRIMONIALES (50 registros)
-- ============================================================
INSERT INTO assets (inventory_number, description, brand_id, model, serial_number, category_id, location_id, invoice_id, entry_date, lifecycle_status, condition_status, created_by, updated_by) VALUES
                                                                                                                                                                                                     ('INV-2026-00005', 'Smartphone POCO X7 Pro', 12, 'X7 Pro', 'SN-POCO-001', 10, 4, 3, '2026-03-01', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00006', 'Smartphone Motorola Moto G54 5G', 11, 'Moto G54', 'SN-MOTO-002', 10, 4, 3, '2026-03-01', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00007', 'Servidor Dell PowerEdge R740', 1, 'PowerEdge R740', 'SN-DELL-SRV1', 9, 4, 4, '2025-02-20', 'AVAILABLE', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00008', 'Switch Cisco Catalyst 2960', 10, 'WS-C2960', 'SN-CIS-001', 15, 5, 5, '2025-03-10', 'ASSIGNED', 'REGULAR', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00009', 'Router MikroTik CCR1009', 16, 'CCR1009-7G', 'SN-MIK-998', 15, 5, 5, '2025-03-10', 'IN_MAINTENANCE', 'BAD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00010', 'Laptop Lenovo ThinkPad T14', 3, 'T14 Gen 2', 'SN-LEN-001', 8, 8, 6, '2025-04-12', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00011', 'Access Point Ubiquiti UniFi AC Pro', 17, 'UAP-AC-PRO', 'SN-UBI-111', 15, 6, 7, '2025-05-15', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00012', 'Monitor HP Z24n', 2, 'Z24n G3', 'SN-HP-M1', 7, 8, 6, '2025-04-12', 'AVAILABLE', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00013', 'Silla Ergonómica Herman Miller', NULL, 'Aeron', NULL, 17, 8, 8, '2025-06-01', 'ASSIGNED', 'REGULAR', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00014', 'Escritorio de Metal en L', NULL, 'ML-200', NULL, 16, 9, 8, '2025-06-01', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00015', 'Licencia IntelliJ IDEA Ultimate', NULL, '2025', 'LIC-JET-001', 19, NULL, 9, '2025-07-01', 'REGISTERED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00016', 'Licencia Windows Server 2022', NULL, 'Standard', 'LIC-WIN-001', 20, NULL, 9, '2025-07-01', 'REGISTERED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00017', 'Laptop Asus ROG Strix', 14, 'G15', 'SN-ASU-001', 8, 10, 10, '2025-08-10', 'AVAILABLE', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00018', 'Laptop Acer Nitro 5', 15, 'AN515', 'SN-ACE-001', 8, 10, 10, '2025-08-10', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00019', 'Impresora Epson EcoTank', 5, 'L3250', 'SN-EPS-002', 7, 11, 11, '2025-09-05', 'IN_MAINTENANCE', 'REGULAR', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00020', 'Pizarrón Blanco Magnético', NULL, '2x1.2m', NULL, 16, 12, 12, '2025-09-20', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00021', 'Microscopio Nikon Optiphot', 24, 'Optiphot-2', 'SN-NIK-001', 22, 13, 13, '2025-10-05', 'AVAILABLE', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00022', 'Osciloscopio Fluke', NULL, '190-204', 'SN-FLU-001', 23, 14, 14, '2025-10-15', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00023', 'Servidor NAS Synology', 28, 'DS920+', 'SN-SYN-001', 24, 4, 15, '2025-11-01', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00024', 'No Break APC 1500VA', 29, 'BR1500G', 'SN-APC-001', 24, 4, 15, '2025-11-01', 'DECOMMISSIONED', 'BAD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00025', 'Pupitre de Polipropileno', NULL, 'PP-100', NULL, 16, 12, 16, '2025-11-10', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00026', 'Pupitre de Polipropileno', NULL, 'PP-100', NULL, 16, 12, 16, '2025-11-10', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00027', 'Pupitre de Polipropileno', NULL, 'PP-100', NULL, 16, 12, 16, '2025-11-10', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00028', 'Pupitre de Polipropileno', NULL, 'PP-100', NULL, 16, 12, 16, '2025-11-10', 'ASSIGNED', 'REGULAR', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00029', 'Pupitre de Polipropileno', NULL, 'PP-100', NULL, 16, 12, 16, '2025-11-10', 'ASSIGNED', 'BAD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00030', 'Proyector Interactivo Epson', 5, 'BrightLink', 'SN-EPS-999', 28, 12, 17, '2025-11-15', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00031', 'Aire Acondicionado LG 2 Ton', 6, 'Inverter', 'SN-LG-AC1', 4, 12, 18, '2025-11-20', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00032', 'Aire Acondicionado LG 2 Ton', 6, 'Inverter', 'SN-LG-AC2', 4, 13, 18, '2025-11-20', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00033', 'Camioneta Nissan NP300', 21, 'NP300', 'VIN-NIS-001', 13, 20, 19, '2025-12-01', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00034', 'Auto Chevrolet Aveo', 22, 'Aveo 2024', 'VIN-CHE-001', 13, 20, 19, '2025-12-01', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00035', 'Cámara de Vigilancia IP', 17, 'UVC-G4-PRO', 'SN-UVC-001', 26, 20, 20, '2025-12-05', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00036', 'Cámara de Vigilancia IP', 17, 'UVC-G4-PRO', 'SN-UVC-002', 26, 20, 20, '2025-12-05', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00037', 'Cámara de Vigilancia IP', 17, 'UVC-G4-PRO', 'SN-UVC-003', 26, 20, 20, '2025-12-05', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00038', 'Taladro Inalámbrico Truper', 19, 'TRU-20V', 'SN-TRU-001', 12, 21, 21, '2025-12-10', 'AVAILABLE', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00039', 'Pulidora Bosch', 20, 'GWS 850', 'SN-BOS-001', 12, 21, 21, '2025-12-10', 'IN_MAINTENANCE', 'REGULAR', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00040', 'Juego de Herramientas 150 pzs', 19, 'JGO-150', NULL, 11, 21, 22, '2025-12-11', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00041', 'Teclado Logitech MX Keys', 8, 'MX Keys', 'SN-LOG-001', 7, 8, 23, '2025-12-15', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00042', 'Mouse Logitech MX Master 3', 8, 'MX Master 3', 'SN-LOG-002', 7, 8, 23, '2025-12-15', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00043', 'Pantalla Interactiva Samsung 65"', 7, 'Flip Pro', 'SN-SAM-001', 29, 14, 24, '2026-01-05', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00044', 'Refrigerador Mabe 14 pies', NULL, 'RMA300', 'SN-MAB-001', 30, 22, 25, '2026-01-10', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00045', 'Microondas Whirlpool', NULL, 'WM18', 'SN-WHI-001', 30, 22, 25, '2026-01-10', 'AVAILABLE', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00046', 'Tractor John Deere', NULL, '5075E', 'VIN-JD-001', 27, 25, 26, '2026-01-20', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00047', 'Balanza Analítica Ohaus', NULL, 'Pioneer', 'SN-OHA-001', 5, 20, 27, '2026-02-01', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00048', 'Centrífuga de Laboratorio', NULL, 'C-100', 'SN-CEN-001', 5, 20, 27, '2026-02-01', 'ASSIGNED', 'REGULAR', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00049', 'Simulador Médico Avanzado', NULL, 'SIM-3000', 'SN-SIM-001', 21, 13, 28, '2026-02-10', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00050', 'Estetoscopio Littmann', NULL, 'Classic III', 'SN-LIT-001', 21, 13, 28, '2026-02-10', 'AVAILABLE', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00051', 'Archivero de Metal 4 Gavetas', NULL, 'AR-4G', NULL, 16, 29, 29, '2026-02-15', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00052', 'Librero de Madera', NULL, 'LIB-2M', NULL, 16, 29, 29, '2026-02-15', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00053', 'Reloj Checador Biométrico', NULL, 'ZK-TECO', 'SN-ZK-001', 2, 30, 30, '2026-02-20', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                     ('INV-2026-00054', 'Impresora de Credenciales Evolis', NULL, 'Primacy', 'SN-EVO-001', 7, 30, 30, '2026-02-20', 'ASSIGNED', 'GOOD', 1, 1);

-- ============================================================
-- 9. ASIGNACIONES (30 registros)
-- ============================================================
INSERT INTO asset_assignments (asset_id, guardian_id, location_id, assigned_by, notes) VALUES
                                                                                           (5, 5, 4, 1, 'Asignado a personal de Sistemas'),
                                                                                           (6, 6, 4, 1, 'Asignado a enfermería para monitoreo'),
                                                                                           (7, 7, 4, 1, 'Servidor para base de datos de producción'),
                                                                                           (8, 7, 5, 1, 'Switch de piso 1'),
                                                                                           (10, 8, 8, 1, 'Equipo docente'),
                                                                                           (11, 7, 6, 1, 'AP para zona aulas'),
                                                                                           (13, 8, 8, 1, 'Silla de trabajo del docente'),
                                                                                           (14, 8, 9, 1, 'Escritorio cubículo'),
                                                                                           (18, 9, 10, 1, 'Equipo portátil para laboratorio'),
                                                                                           (20, 10, 12, 1, 'Pizarrón del aula Múltiple 1'),
                                                                                           (22, 11, 14, 1, 'Osciloscopio para electrónica'),
                                                                                           (23, 7, 4, 1, 'NAS para respaldos institucionales'),
                                                                                           (25, 10, 12, 1, 'Pupitre aula 1'),
                                                                                           (26, 10, 12, 1, 'Pupitre aula 1'),
                                                                                           (27, 10, 12, 1, 'Pupitre aula 1'),
                                                                                           (28, 10, 12, 1, 'Pupitre aula 1'),
                                                                                           (29, 10, 12, 1, 'Pupitre aula 1'),
                                                                                           (30, 10, 12, 1, 'Proyector aula 1'),
                                                                                           (31, 10, 12, 1, 'Clima aula 1'),
                                                                                           (32, 11, 13, 1, 'Clima aula 2'),
                                                                                           (33, 12, 20, 1, 'Vehículo utilitario de mantenimiento'),
                                                                                           (34, 13, 20, 1, 'Vehículo de rectoría'),
                                                                                           (35, 14, 20, 1, 'Cámara acceso principal'),
                                                                                           (36, 14, 20, 1, 'Cámara acceso lateral'),
                                                                                           (37, 14, 20, 1, 'Cámara estacionamiento'),
                                                                                           (40, 12, 21, 1, 'Kit de herramientas para electricista'),
                                                                                           (41, 8, 8, 1, 'Teclado asignado'),
                                                                                           (42, 8, 8, 1, 'Mouse asignado'),
                                                                                           (43, 15, 14, 1, 'Pantalla para sala de juicios/titulación'),
                                                                                           (44, 16, 22, 1, 'Refrigerador para insumos de cafetería');

-- ============================================================
-- 10. INCIDENCIAS (30 registros)
-- ============================================================
INSERT INTO incidents (asset_id, incident_date, description, repair_type, status, condition_at_incident, created_by) VALUES
                                                                                                                         (9, '2026-03-01', 'El router presenta reinicios aleatorios cada 4 horas.', 'EXTERNAL', 'OPEN', 'BAD', 3),
                                                                                                                         (19, '2026-03-02', 'Los cabezales de la impresora están obstruidos, imprime con rayas.', 'INTERNAL', 'IN_PROGRESS', 'REGULAR', 4),
                                                                                                                         (24, '2026-03-03', 'Las baterías del UPS se hincharon y derramaron ácido.', 'EXTERNAL', 'CLOSED', 'BAD', 3),
                                                                                                                         (29, '2026-03-04', 'Pupitre con la paleta de escritura rota.', 'INTERNAL', 'OPEN', 'BAD', 4),
                                                                                                                         (39, '2026-03-05', 'La pulidora hace corto al presionar el gatillo.', 'INTERNAL', 'IN_PROGRESS', 'BAD', 3),
                                                                                                                         (10, '2026-03-06', 'Pantalla parpadea intermitentemente.', 'INTERNAL', 'OPEN', 'REGULAR', 3),
                                                                                                                         (13, '2026-03-07', 'Rodaja izquierda atorada.', 'INTERNAL', 'RESOLVED', 'REGULAR', 4),
                                                                                                                         (28, '2026-03-08', 'Tornillos sueltos en el respaldo.', 'INTERNAL', 'RESOLVED', 'REGULAR', 3),
                                                                                                                         (44, '2026-03-09', 'El congelador no enfría lo suficiente.', 'EXTERNAL', 'OPEN', 'BAD', 4),
                                                                                                                         (11, '2026-03-10', 'No emite señal de radio en 5GHz.', 'INTERNAL', 'IN_PROGRESS', 'REGULAR', 3),
                                                                                                                         (48, '2026-03-11', 'Vibración excesiva durante el centrifugado.', 'EXTERNAL', 'OPEN', 'BAD', 4),
                                                                                                                         (31, '2026-03-12', 'Gotea agua en la unidad interior.', 'INTERNAL', 'RESOLVED', 'REGULAR', 3),
                                                                                                                         (8, '2026-03-13', 'Puerto 15 y 16 no dan link.', 'EXTERNAL', 'OPEN', 'BAD', 4),
                                                                                                                         (33, '2026-03-14', 'Testigo de motor encendido (Check Engine).', 'EXTERNAL', 'IN_PROGRESS', 'REGULAR', 3),
                                                                                                                         (46, '2026-03-15', 'Fuga de aceite hidráulico.', 'EXTERNAL', 'OPEN', 'BAD', 4),
                                                                                                                         (49, '2026-03-16', 'Fallo de calibración en brazo derecho del simulador.', 'EXTERNAL', 'OPEN', 'REGULAR', 3),
                                                                                                                         (37, '2026-03-17', 'Lente empañado por humedad.', 'INTERNAL', 'RESOLVED', 'REGULAR', 4),
                                                                                                                         (14, '2026-03-18', 'Gaveta inferior atascada.', 'INTERNAL', 'RESOLVED', 'REGULAR', 3),
                                                                                                                         (35, '2026-03-19', 'Pérdida de conexión PoE.', 'INTERNAL', 'IN_PROGRESS', 'REGULAR', 4),
                                                                                                                         (22, '2026-03-20', 'Display con píxeles muertos.', 'EXTERNAL', 'OPEN', 'BAD', 3),
                                                                                                                         (5, '2026-03-21', 'Puerto de carga tipo C con falso contacto.', 'EXTERNAL', 'OPEN', 'REGULAR', 4),
                                                                                                                         (42, '2026-03-22', 'Scroll no responde.', 'INTERNAL', 'RESOLVED', 'REGULAR', 3),
                                                                                                                         (18, '2026-03-23', 'Teclado retroiluminado no enciende.', 'INTERNAL', 'IN_PROGRESS', 'REGULAR', 4),
                                                                                                                         (30, '2026-03-24', 'Foco del proyector fundido.', 'INTERNAL', 'OPEN', 'BAD', 3),
                                                                                                                         (43, '2026-03-25', 'Touch descalibrado en esquina superior.', 'EXTERNAL', 'OPEN', 'REGULAR', 4),
                                                                                                                         (23, '2026-03-26', 'Ventilador de NAS suena muy fuerte.', 'INTERNAL', 'RESOLVED', 'REGULAR', 3),
                                                                                                                         (34, '2026-03-27', 'Batería muerta, no arranca.', 'INTERNAL', 'RESOLVED', 'BAD', 4),
                                                                                                                         (53, '2026-03-28', 'No reconoce huellas secas.', 'INTERNAL', 'IN_PROGRESS', 'REGULAR', 3),
                                                                                                                         (54, '2026-03-29', 'Atasca las tarjetas de PVC.', 'INTERNAL', 'OPEN', 'REGULAR', 4),
                                                                                                                         (6, '2026-03-30', 'Batería se descarga en menos de 2 horas.', 'EXTERNAL', 'OPEN', 'BAD', 3);

-- ============================================================
-- 11. BAJAS (10 registros)
-- ============================================================
INSERT INTO asset_decommissions (asset_id, incident_id, justification, decommission_date, status, created_by, confirmed_by, confirmed_at) VALUES
                                                                                                                                              (24, 3, 'Daño irreparable en circuitería por derrame de ácido. Dictamen técnico anexo.', '2026-04-01', 'CONFIRMED', 3, 1, '2026-04-05'),
                                                                                                                                              (29, 4, 'Pupitre irrecuperable, paleta rota y estructura metálica oxidada.', '2026-04-02', 'PENDING', 4, NULL, NULL);

INSERT INTO asset_decommissions (asset_id, incident_id, justification, decommission_date, status, created_by) VALUES
                                                                                                                  (45, NULL, 'Obsolescencia tecnológica dictaminada por área usuaria.', '2026-04-10', 'PENDING', 3),
                                                                                                                  (50, NULL, 'Extravío reportado en acta de hechos.', '2026-04-11', 'PENDING', 4),
                                                                                                                  (51, NULL, 'Destrucción por inundación en archivo.', '2026-04-12', 'PENDING', 3),
                                                                                                                  (52, NULL, 'Destrucción por termitas comprobada.', '2026-04-13', 'PENDING', 4),
                                                                                                                  (2,  NULL, 'Monitor estrellado en mudanza interna.', '2026-04-14', 'PENDING', 3),
                                                                                                                  (3,  NULL, 'Mobiliario roto y desmantelado.', '2026-04-15', 'PENDING', 4),
                                                                                                                  (12, NULL, 'Falla de panel LCD tras años de uso, reparación incosteable.', '2026-04-16', 'PENDING', 3),
                                                                                                                  (17, NULL, 'Equipo dañado por descarga eléctrica severa.', '2026-04-17', 'PENDING', 4);

-- ============================================================
-- 12. MANTENIMIENTOS (30 registros)
-- ============================================================
INSERT INTO maintenance_logs (asset_id, maintenance_type, description, performed_by, performed_date, cost, condition_before, condition_after, created_by) VALUES
                                                                                                                                                              (7, 'PREVENTIVE', 'Mantenimiento semestral: limpieza de disipadores y actualización de firmware iDRAC.', 'Soporte TI', '2025-08-20', 0.00, 'GOOD', 'GOOD', 1),
                                                                                                                                                              (33, 'PREVENTIVE', 'Afinación mayor, cambio de bujías, aceite y filtros.', 'Agencia Nissan', '2026-01-10', 4500.00, 'GOOD', 'GOOD', 2),
                                                                                                                                                              (31, 'PREVENTIVE', 'Limpieza de serpentines y filtros de aire.', 'Mantenimiento General', '2026-02-15', 0.00, 'REGULAR', 'GOOD', 1),
                                                                                                                                                              (13, 'CORRECTIVE', 'Cambio de rodaja atascada por acumulación de pelusa.', 'Mantenimiento General', '2026-03-08', 150.00, 'REGULAR', 'GOOD', 2),
                                                                                                                                                              (37, 'CORRECTIVE', 'Sellado de carcasa exterior por filtración de humedad.', 'Soporte TI', '2026-03-18', 0.00, 'REGULAR', 'GOOD', 1),
                                                                                                                                                              (14, 'CORRECTIVE', 'Ajuste de rieles de gaveta.', 'Mantenimiento General', '2026-03-19', 0.00, 'REGULAR', 'GOOD', 1),
                                                                                                                                                              (23, 'CORRECTIVE', 'Limpieza profunda de ventilador de NAS.', 'Soporte TI', '2026-03-27', 0.00, 'REGULAR', 'GOOD', 2),
                                                                                                                                                              (34, 'CORRECTIVE', 'Cambio de acumulador (batería) del vehículo.', 'Taller Externo', '2026-03-28', 2100.00, 'BAD', 'GOOD', 1),
                                                                                                                                                              (42, 'CORRECTIVE', 'Limpieza de sensor de scroll con alcohol isopropílico.', 'Soporte TI', '2026-03-23', 0.00, 'REGULAR', 'GOOD', 2),
                                                                                                                                                              (28, 'CORRECTIVE', 'Reapriete de tornillería en respaldo.', 'Mantenimiento General', '2026-03-09', 0.00, 'REGULAR', 'GOOD', 1),
                                                                                                                                                              (10, 'WARRANTY', 'Cambio de motherboard por fallo en puertos USB.', 'Garantía Lenovo', '2025-10-10', 0.00, 'BAD', 'GOOD', 2),
                                                                                                                                                              (11, 'PREVENTIVE', 'Actualización de firmware de controlador UniFi.', 'Soporte TI', '2025-11-15', 0.00, 'GOOD', 'GOOD', 1),
                                                                                                                                                              (8, 'PREVENTIVE', 'Sopleteado de puertos y revisión de conectividad.', 'Soporte TI', '2025-12-05', 0.00, 'GOOD', 'GOOD', 2),
                                                                                                                                                              (46, 'PREVENTIVE', 'Servicio a tractor de 100 horas.', 'Agencia John Deere', '2026-02-25', 5600.00, 'GOOD', 'GOOD', 1),
                                                                                                                                                              (47, 'PREVENTIVE', 'Calibración anual con pesas certificadas.', 'Metrología Externa', '2026-03-01', 3200.00, 'GOOD', 'GOOD', 2),
                                                                                                                                                              (35, 'PREVENTIVE', 'Limpieza de lente exterior.', 'Soporte TI', '2026-01-20', 0.00, 'GOOD', 'GOOD', 1),
                                                                                                                                                              (36, 'PREVENTIVE', 'Limpieza de lente exterior.', 'Soporte TI', '2026-01-20', 0.00, 'GOOD', 'GOOD', 2),
                                                                                                                                                              (38, 'PREVENTIVE', 'Engrasado de broquero.', 'Mantenimiento General', '2026-02-10', 0.00, 'GOOD', 'GOOD', 1),
                                                                                                                                                              (40, 'PREVENTIVE', 'Inventario y limpieza del maletín de herramientas.', 'Mantenimiento General', '2026-02-11', 0.00, 'GOOD', 'GOOD', 2),
                                                                                                                                                              (43, 'PREVENTIVE', 'Actualización de SO de pantalla Samsung.', 'Soporte TI', '2026-03-10', 0.00, 'GOOD', 'GOOD', 1),
                                                                                                                                                              (44, 'PREVENTIVE', 'Descongelamiento y limpieza interna.', 'Cafetería', '2026-02-28', 0.00, 'GOOD', 'GOOD', 2),
                                                                                                                                                              (20, 'PREVENTIVE', 'Limpieza profunda con líquido especial para pizarrones.', 'Intendencia', '2026-01-15', 0.00, 'GOOD', 'GOOD', 1),
                                                                                                                                                              (21, 'PREVENTIVE', 'Limpieza de ópticas de microscopio.', 'Laboratorista', '2026-02-05', 0.00, 'GOOD', 'GOOD', 2),
                                                                                                                                                              (25, 'PREVENTIVE', 'Revisión y apriete de tornillos.', 'Mantenimiento General', '2026-01-30', 0.00, 'GOOD', 'GOOD', 1),
                                                                                                                                                              (26, 'PREVENTIVE', 'Revisión y apriete de tornillos.', 'Mantenimiento General', '2026-01-30', 0.00, 'GOOD', 'GOOD', 2),
                                                                                                                                                              (27, 'PREVENTIVE', 'Revisión y apriete de tornillos.', 'Mantenimiento General', '2026-01-30', 0.00, 'GOOD', 'GOOD', 1),
                                                                                                                                                              (51, 'PREVENTIVE', 'Lubricación de rieles de gavetas metálicas.', 'Mantenimiento General', '2026-03-05', 0.00, 'GOOD', 'GOOD', 2),
                                                                                                                                                              (53, 'PREVENTIVE', 'Actualización de firmware de checador.', 'Soporte TI', '2026-03-15', 0.00, 'GOOD', 'GOOD', 1),
                                                                                                                                                              (54, 'PREVENTIVE', 'Limpieza de rodillos de impresora Evolis con kit oficial.', 'Soporte TI', '2026-03-20', 0.00, 'GOOD', 'GOOD', 2),
                                                                                                                                                              (1, 'PREVENTIVE', 'Cambio de pasta térmica (Mantenimiento Anual).', 'Soporte TI', '2026-04-01', 0.00, 'GOOD', 'GOOD', 1);