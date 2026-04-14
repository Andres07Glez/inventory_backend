package mx.edu.unpa.inventory_backend.enums;

public enum LifecycleStatus {
    REGISTERED,      // Recién dado de alta, pendiente de disponibilidad
    AVAILABLE,       // Disponible para ser asignado
    ASSIGNED,        // Asignado a un resguardante
    IN_MAINTENANCE,  // En proceso de reparación o mantenimiento
    IN_WARRANTY,     // En garantía con el proveedor
    DECOMMISSIONED   // Dado de baja definitivamente (estado final)
}