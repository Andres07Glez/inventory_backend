package mx.edu.unpa.inventory_backend.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
class InventoryNumberGeneratorTest {

    private InventoryNumberGenerator generator;

    @BeforeEach
    void setUp() {
        // No se pasa AssetRepository porque generate() no lo invoca.
        // Si se elimina la dependencia en producción, este test seguirá compilando.
        generator = new InventoryNumberGenerator();
    }

    // =========================================================================
    // generate()
    // =========================================================================

    @Test
    void should_returnFormattedInventoryNumber_when_validAssetIdProvided() {
        // Arrange
        Long assetId     = 1L;
        int  currentYear = Year.now().getValue();
        String expected  = String.format("INV-%d-00001", currentYear);

        // Act
        String result = generator.generate(assetId);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void should_padAssetIdWithLeadingZeros_when_idHasLessThanFiveDigits() {
        // Arrange — IDs pequeños deben quedar con ceros a la izquierda
        int currentYear = Year.now().getValue();

        // Act & Assert
        assertThat(generator.generate(1L))
                .isEqualTo(String.format("INV-%d-00001", currentYear));
        assertThat(generator.generate(42L))
                .isEqualTo(String.format("INV-%d-00042", currentYear));
        assertThat(generator.generate(999L))
                .isEqualTo(String.format("INV-%d-00999", currentYear));
    }

    @Test
    void should_notPadAssetId_when_idHasFiveOrMoreDigits() {
        // Edge case: IDs grandes no deben truncarse — el formato %05d solo
        // agrega padding, no corta dígitos.
        int currentYear = Year.now().getValue();

        assertThat(generator.generate(10_000L))
                .isEqualTo(String.format("INV-%d-10000", currentYear));
        assertThat(generator.generate(999_999L))
                .isEqualTo(String.format("INV-%d-999999", currentYear));
    }

    @Test
    void should_containCurrentYear_when_generated() {
        // Determinístico: captura el año en Arrange para que el assert no dependa
        // de cuándo exactamente se ejecuta (ej. cerca de medianoche de año nuevo).
        int currentYear = Year.now().getValue();

        String result = generator.generate(1L);

        assertThat(result).contains(String.valueOf(currentYear));
    }

    @Test
    void should_matchExpectedFormat_when_anyValidIdProvided() {
        // Verifica la estructura INV-{4 dígitos de año}-{5+ dígitos} con regex
        String result = generator.generate(5L);

        assertThat(result).matches("INV-\\d{4}-\\d{5,}");
    }

    @Test
    void should_generateDifferentNumbers_when_differentIdsProvided() {
        // Dos assets distintos nunca deben producir el mismo número de inventario
        String first  = generator.generate(1L);
        String second = generator.generate(2L);

        assertThat(first).isNotEqualTo(second);
    }
}