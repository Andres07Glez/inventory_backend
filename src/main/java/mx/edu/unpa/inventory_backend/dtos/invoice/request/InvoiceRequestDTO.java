package mx.edu.unpa.inventory_backend.dtos.invoice.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class InvoiceRequestDTO {

    @NotBlank(message = "El número de factura es obligatorio.")
    @Size(max = 100, message = "El número de factura no puede superar 100 caracteres.")
    private String invoiceNumber;

    @Size(max = 200, message = "El proveedor no puede superar 200 caracteres.")
    private String supplier;

    @NotNull(message = "La fecha de factura es obligatoria.")
    private LocalDate invoiceDate;

    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a 0.")
    @Digits(integer = 10, fraction = 2, message = "Formato de monto inválido.")
    private BigDecimal totalAmount;

    @Size(max = 500, message = "La ruta del documento no puede superar 500 caracteres.")
    private String documentPath;

    @Size(max = 1000, message = "Las notas no pueden superar 1000 caracteres.")
    private String notes;

}
