package mx.edu.unpa.inventory_backend.DTOs.Asset.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AssetRequestDTO {

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
    private String description;

    @Size(max = 100)
    private String brand;

    @Size(max = 150)
    private String model;

    @Size(max = 200)
    private String serialNumber;

    private String barcode;

    private String notes;

    @NotNull(message = "La categoría es obligatoria")
    private Integer categoryId;

    @NotNull(message = "La ubicación es obligatoria")
    private Integer locationId;

    private Long invoiceId;

    @NotNull(message = "La fecha de entrada es obligatoria")
    private LocalDate entryDate;

    // Estos campos suelen ser opcionales al registro inicial
    private String conditionStatus; // Se puede recibir como String y validar en el Service

}
