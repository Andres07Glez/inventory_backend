package mx.edu.unpa.inventory_backend.exceptions;

/**
 * Se lanza cuando el StorageService no puede guardar o eliminar un archivo.
 *
 * Nota: el nombre conserva el typo original del GlobalExceptionHandler
 * ("Exeption" en lugar de "Exception") para no romper el handler ya existente.
 *
 * Manejada por GlobalExceptionHandler → HTTP 500 INTERNAL_SERVER_ERROR.
 */
public class FileStorageExeption extends RuntimeException {

    public FileStorageExeption(String message) {
        super(message);
    }

    public FileStorageExeption(String message, Throwable cause) {
        super(message, cause);
    }
}
