package mx.edu.unpa.inventory_backend.dtos.asset.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssetResponseDTO {

    private Long id;
    private String inventoryNumber; // El folio generado (ej: INV-2026-0001)
    private String barcode;
    private String description;
    private String brand;
    private String model;
    private String serialNumber;
    private String notes;

    // Información detallada de relaciones (No solo el ID)
    private String categoryName;
    private String locationName;
    private String campus; // Importante para filtros en la UNPA
    private String invoiceNumber;

    private LocalDate entryDate;
    private String conditionStatus;
    private String lifecycleStatus;

    // Datos de auditoría para visualización
    private LocalDateTime createdAt;
    private String createdByName; // Nombre completo del usuario que registró

    // Lista de rutas de imágenes si existen
    private List<String> imageUrls;

}
