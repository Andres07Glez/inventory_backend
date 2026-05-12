package mx.edu.unpa.inventory_backend;

import mx.edu.unpa.inventory_backend.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class InventoryBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryBackendApplication.class, args);
    }

}
