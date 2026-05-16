package mx.edu.unpa.inventory_backend.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
     /** Almacena el archivo. Retorna ruta relativa para persistir en DB. */
     String store(MultipartFile file, String subDir);
     /** Elimina el archivo físico. Idempotente: no lanza si ya no existe. */
     void delete(String relativePath);
     String buildPublicUrl(String relativePath);
 }
