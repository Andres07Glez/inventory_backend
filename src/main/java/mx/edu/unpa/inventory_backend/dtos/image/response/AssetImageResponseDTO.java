package mx.edu.unpa.inventory_backend.dtos.image.response;

 public record AssetImageResponseDTO(
         Long   id,
         String fileName,
         String url,          // URL pública construida por StorageService
         String mimeType,
         boolean isPrimary
 ) {}
