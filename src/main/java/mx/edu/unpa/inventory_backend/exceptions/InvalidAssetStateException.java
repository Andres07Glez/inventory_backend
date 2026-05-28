package mx.edu.unpa.inventory_backend.exceptions;

public class InvalidAssetStateException extends RuntimeException {

    public InvalidAssetStateException(String message) {
        super(message);
    }
}