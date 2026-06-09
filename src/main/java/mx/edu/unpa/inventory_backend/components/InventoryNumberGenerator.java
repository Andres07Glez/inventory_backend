package mx.edu.unpa.inventory_backend.components;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
@RequiredArgsConstructor
public class InventoryNumberGenerator {


    /**
     * Genera el número de inventario institucional en formato INV-{AÑO}-{ID}.
     *
     * Estrategia: se llama DESPUÉS del primer save() para usar el ID
     * autoincremental ya asignado por la BD, evitando condiciones de carrera.
     *
     *
     * @param assetId el ID ya persistido del bien
     * @return número de inventario formateado
     */
    public String generate(Long assetId) {
        int year = Year.now().getValue();
        return String.format("INV-%d-%05d", year, assetId);
    }
}