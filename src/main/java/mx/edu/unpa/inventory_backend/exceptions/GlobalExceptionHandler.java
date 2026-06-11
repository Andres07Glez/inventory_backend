package mx.edu.unpa.inventory_backend.exceptions;

import lombok.extern.slf4j.Slf4j;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;


import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 404 — Recurso no encontrado */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** 409 — Operación inválida por estado del bien */
    @ExceptionHandler(InvalidAssetStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidAssetState(InvalidAssetStateException ex) {
        log.warn("Operación inválida por estado del bien: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** 409 — Transición de estado inválida en incidencia */
    @ExceptionHandler(InvalidIncidentStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidIncidentState(InvalidIncidentStateException ex) {
        log.warn("Operación inválida por estado de incidencia: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** 409 — Operación inválida sobre una baja de bien */
    @ExceptionHandler(InvalidDecommissionStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidDecommissionState(InvalidDecommissionStateException ex) {
        log.warn("Operación inválida sobre baja de bien: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** 500 — Error al almacenar o eliminar un archivo */
    @ExceptionHandler(FileStorageExeption.class)
    public ResponseEntity<ApiResponse<Void>> handleFileStorage(FileStorageExeption ex) {
        log.error("Error de almacenamiento de archivo: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("No se pudo guardar el documento adjunto."));
    }

    /** 400 — Violación de @Valid en @RequestBody */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Error de validación");

        log.warn("Error de validación: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /** 400 — Violación de @Validated en @RequestParam / @PathVariable */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .findFirst()
                .orElse("Parámetro inválido");

        log.warn("Violación de constraint: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /** 400 — @RequestParam requerido no enviado */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = "Parámetro requerido ausente: " + ex.getParameterName();
        log.warn(message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /** 409 — Recurso duplicado */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateResourceException ex) {
        log.warn("Recurso duplicado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** 401 — Credenciales inválidas o token expirado */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Autenticación fallida: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Credenciales inválidas o sesión expirada."));
    }

    /** 403 — Autenticado pero sin permisos */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tiene permisos para realizar esta operación."));
    }

    /** 500 — Error inesperado. Log completo en servidor, mensaje genérico al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericError(Exception ex) {
        log.error("Error inesperado: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Ocurrió un error interno. Contacte al administrador."));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        // Puedes cambiar el "String" por tu clase de respuesta de error estándar (ej. ErrorResponseDTO)
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("El cuerpo de la petición es requerido o el formato JSON es inválido.");
    }

    // ──────────────────────────────────────────────────────────────────────────────
// PARCHE para GlobalExceptionHandler.java
//
// Spring Boot 4 / Spring Framework 7 introdujo HandlerMethodValidationException
// para violaciones de constraints en @PathVariable y @RequestParam (ej. @Positive).
// Esta excepción es DISTINTA de ConstraintViolationException y debe ser mapeada
// explícitamente, o de lo contrario cae en el handler genérico → HTTP 500.
//
// También se agrega MissingServletRequestPartException para cuando falta
// el campo de un multipart/form-data (ej. el campo "file" en el upload).
//
// Agregar estos dos handlers DENTRO de GlobalExceptionHandler, junto al resto:
// ──────────────────────────────────────────────────────────────────────────────

    /**
     * 400 — Violación de @Positive / @Min / @Max en @PathVariable o @RequestParam.
     *
     * Spring 7 lanza HandlerMethodValidationException en lugar de
     * ConstraintViolationException cuando las constraints están en parámetros
     * de métodos de controladores (path variables, request params).
     * Sin este handler, la excepción llega al catch-all de Exception → HTTP 500.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {

        // Usamos getAllErrors() directamente, lo cual es mucho más seguro a nivel de compilación
        String errorMessage = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        Map<String, Object> responseBody = Map.of(
                "success", false,
                "message", errorMessage
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
    }

    /**
     * 400 — Parte requerida de un multipart/form-data no encontrada.
     *
     * Se lanza cuando @RequestParam("file") MultipartFile recibe una petición
     * multipart sin el campo esperado (campo ausente o con nombre incorrecto).
     */
    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPart(
            org.springframework.web.multipart.support.MissingServletRequestPartException ex) {
        String message = "Parte requerida ausente: " + ex.getRequestPartName();
        log.warn(message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            org.springframework.web.server.ResponseStatusException ex) {
        String message = ex.getReason() != null
                ? ex.getReason()
                : ex.getMessage();
        log.warn("ResponseStatusException [{}]: {}", ex.getStatusCode(), message);
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ApiResponse.error(message));
    }

}

