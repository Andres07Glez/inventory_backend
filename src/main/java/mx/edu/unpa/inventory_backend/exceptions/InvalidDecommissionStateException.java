package mx.edu.unpa.inventory_backend.exceptions;

/**
        * Se lanza cuando se intenta una operación incompatible con el estado
 * actual de una baja (AssetDecommission).
        *
        * Ejemplos:
        *   - Confirmar una baja que ya está CONFIRMED
 *   - Crear una baja sobre un bien ya DECOMMISSIONED
 *   - Crear una segunda baja para el mismo bien
 *
         * Manejada por GlobalExceptionHandler → HTTP 409 CONFLICT.
 */
public class InvalidDecommissionStateException extends RuntimeException {

    public InvalidDecommissionStateException(String message) {
        super(message);
    }
}

