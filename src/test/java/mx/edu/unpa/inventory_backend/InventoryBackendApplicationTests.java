package mx.edu.unpa.inventory_backend;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InventoryBackendApplicationTests {

    @Test
    void contextLoads() {
        Assertions.assertTrue(true,"El contexto de Spring debería cargar sin problemas");
    }

}
