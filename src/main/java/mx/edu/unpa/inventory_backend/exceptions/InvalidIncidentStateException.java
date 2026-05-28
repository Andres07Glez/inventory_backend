package mx.edu.unpa.inventory_backend.exceptions;

/**
 * Se lanza cuando se intenta una transición de estado inválida sobre una incidencia,
 * o cuando se realizan operaciones incompatibles con su estado actual.
 *
 * Manejada por GlobalExceptionHandler → HTTP 409 CONFLICT.
 */
public class InvalidIncidentStateException extends RuntimeException {

    public InvalidIncidentStateException(String message) {
        super(message);
    }
}

