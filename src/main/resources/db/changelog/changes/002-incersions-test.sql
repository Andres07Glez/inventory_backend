--liquibase formatted sql
--changeset equipo:002-incersions-test.sql
--comment: Registros para desarrollo

-- 1. USUARIOS (Password: 'password123' - Hash genérico para pruebas)
INSERT INTO users (username, email, password_hash, full_name, employee_number, role) VALUES
                                                                                         ('admin_karen', 'karen.jimenez@unpa.edu.mx', '$2a$10$cGgoScHbjEMsVTbcVYd/Vuja8kG2WDFp980EXiaDR1XNtLUE0EKn.', 'Karen Jimenez Rendon', 'EMP-001', 'ADMIN'),
                                                                                         ('operador1', 'operador@unpa.edu.mx', '$2a$10$6OILz/FcaDvshuxRufya1ORKpNp079x4Kp8ZqIyFs6zeLweQJqO16', 'Juan Pérez', 'EMP-002', 'USER');

-- 2. UBICACIONES (Campus Loma Bonita)
INSERT INTO locations (name, building, campus, description) VALUES
                                                                ('Laboratorio de Cómputo A', 'Edificio L', 'Loma Bonita', 'Planta Alta, Aula 5'),
                                                                ('Oficina de Zootecnia', 'Edificio Administrativo', 'Loma Bonita', 'Cubículo 3'),
                                                                ('Almacén General', 'Bodega Central', 'Loma Bonita', 'Área de recepción de bienes');

-- 3. RESGUARDANTES (Guardians)
INSERT INTO guardians (employee_number, full_name, email, department) VALUES
                                                                          ('DOC-100', 'M.E. Yesenia Barrientos Arenal', 'ybarrientos@unpa.edu.mx', 'Zootecnia'),
                                                                          ('DOC-101', 'Dr. Roberto García', 'rgarcia@unpa.edu.mx', 'Sistemas Automotrices'),
                                                                          ('DOC-102', 'M.C. Laura Torres', 'ltorres@unpa.edu.mx', 'Investigación');

-- 4. FACTURAS (Invoices)
INSERT INTO invoices (invoice_number, supplier, invoice_date, total_amount, created_by) VALUES
                                                                                            ('FACT-2026-A1', 'Dell México S.A. de C.V.', '2026-01-15', 45000.00, 1),
                                                                                            ('FACT-2026-B2', 'Office Depot', '2026-02-10', 12500.50, 1);

-- 5. BIENES (Assets)
-- Se asocian a las categorías creadas en tu script original (8=Laptops, 7=Periféricos, 1=Mobiliario)
INSERT INTO assets (inventory_number, description, brand, model, serial_number, category_id, location_id, invoice_id, entry_date, lifecycle_status, condition_status, created_by, updated_by) VALUES
                                                                                                                                                                                                  ('INV-2026-001', 'Laptop Dell Latitude 5420', 'Dell', 'Latitude 5420', 'SN-DELL-001', 8, 1, 1, '2026-01-20', 'ASSIGNED', 'GOOD', 1, 1),
                                                                                                                                                                                                  ('INV-2026-002', 'Monitor LG 24 Pulgadas', 'LG', '24MK430H', 'SN-LG-992', 7, 1, 1, '2026-01-20', 'AVAILABLE', 'GOOD', 1, 1),
                                                                                                                                                                                                  ('INV-2026-003', 'Escritorio Ejecutivo Madera', 'Generic', 'Mod-2024', NULL, 1, 2, 2, '2026-02-15', 'ASSIGNED', 'REGULAR', 1, 1),
                                                                                                                                                                                                  ('INV-2026-004', 'Proyector Epson', 'Epson', 'PowerLite', 'SN-EPS-332', 7, 3, 2, '2026-02-15', 'IN_MAINTENANCE', 'BAD', 1, 1);

-- 6. ASIGNACIONES (Asset Assignments)
-- Asignamos la Laptop (ID 1) a Yesenia Barrientos (ID 1)
INSERT INTO asset_assignments (asset_id, guardian_id, location_id, assigned_by, notes) VALUES
                                                                                           (1, 1, 1, 1, 'Equipo entregado para labores docentes'),
                                                                                           (3, 1, 2, 1, 'Mobiliario de oficina asignado');

-- 7. INCIDENCIAS (Incidents)
-- Reportamos falla en el proyector (ID 4)
INSERT INTO incidents (asset_id, description, status, condition_at_incident, created_by) VALUES
    (4, 'El lente del proyector muestra una mancha amarilla constante.', 'OPEN', 'BAD', 2);

-- 8. MANTENIMIENTO (Maintenance Logs)
-- Registro de limpieza preventiva para la laptop (ID 1)
INSERT INTO maintenance_logs (asset_id, maintenance_type, description, performed_by, performed_date, cost, condition_before, condition_after, created_by) VALUES
    (1, 'PREVENTIVE', 'Limpieza interna y cambio de pasta térmica', 'Soporte Técnico UNPA', '2026-03-01', 0.00, 'GOOD', 'GOOD', 1);