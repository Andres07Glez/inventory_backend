package mx.edu.unpa.inventory_backend.enums;

/**
 * Estado del proceso de baja de un bien patrimonial.
 *
 * Flujo:
 *   PENDING   → El proceso de baja fue iniciado pero aún no está confirmado.
 *               Un OPERADOR puede iniciarla; un ADMIN debe confirmarla.
 *   CONFIRMED → La baja fue autorizada y el bien queda DECOMMISSIONED de forma definitiva.
 *
 * Nota: No existe estado CANCELLED en este flujo porque una baja cancelada
 * simplemente no se guarda (o se elimina el registro PENDING antes de confirmar).
 * Si el negocio requiere auditoría de bajas canceladas en el futuro, se puede
 * agregar aquí sin impacto en el resto del sistema.
 */
public enum DecommissionStatus {
    PENDING,
    CONFIRMED
}

