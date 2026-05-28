package mx.edu.unpa.inventory_backend.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(String uploadDir,String baseUrl, List<String> allowedMimeTypes) {}
