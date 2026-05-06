-- ============================================================
-- SP-04 FIX: Ubicación heredada del resguardante
-- Agrega la columna location_id a la tabla guardians.
-- ============================================================

ALTER TABLE guardians
    ADD COLUMN location_id INT NULL,
    ADD CONSTRAINT fk_guardians_location
        FOREIGN KEY (location_id) REFERENCES locations (id)
            ON UPDATE CASCADE
            ON DELETE SET NULL;

-- Índice para acelerar la búsqueda de resguardantes por ubicación
CREATE INDEX idx_guardians_location ON guardians (location_id);

-- ============================================================
-- MIGRACIÓN DE DATOS EXISTENTES (opcional)
-- Si ya hay asignaciones activas, podemos poblar location_id
-- en guardians tomando la última ubicación asignada a cada uno.
-- Ejecutar solo si hay datos que migrar.
-- ============================================================
/*
UPDATE guardians g
    JOIN (
        SELECT aa.guardian_id, aa.location_id
        FROM asset_assignments aa
        INNER JOIN (
            SELECT guardian_id, MAX(assigned_at) AS max_assigned
            FROM asset_assignments
            WHERE returned_at IS NULL
            GROUP BY guardian_id
        ) latest ON aa.guardian_id = latest.guardian_id
                 AND aa.assigned_at = latest.max_assigned
        WHERE aa.returned_at IS NULL
    ) last_assignment ON g.id = last_assignment.guardian_id
SET g.location_id = last_assignment.location_id
WHERE g.location_id IS NULL;
*/
