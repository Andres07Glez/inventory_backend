package mx.edu.unpa.inventory_backend.servicesImpl;

import mx.edu.unpa.inventory_backend.domains.Brand;
import mx.edu.unpa.inventory_backend.dtos.brand.request.BrandRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.brand.response.BrandResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.repositories.BrandRepository;
import mx.edu.unpa.inventory_backend.services.impl.BrandServiceImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplTest {

    @Mock private BrandRepository brandRepository;
    @Mock private AssetRepository assetRepository;

    @InjectMocks
    private BrandServiceImpl brandService;

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Brand no usa @Builder, así que lo construimos con setters.
     * No llamamos a @PrePersist manualmente; en tests unitarios JPA no está activo.
     */
    private Brand buildBrand(Integer id, String name, boolean isActive) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setName(name);
        brand.setIsActive(isActive);
        return brand;
    }

    private BrandRequestDTO buildRequest(String name) {
        BrandRequestDTO dto = new BrandRequestDTO();
        dto.setName(name);
        return dto;
    }

    // =========================================================================
    // getAllActive()
    // =========================================================================
    @Nested
    class GetAllActive {

        @Test
        void should_returnMappedDTOList_when_activeBrandsExist() {
            // Arrange
            List<Brand> brands = List.of(
                    buildBrand(1, "Apple",  true),
                    buildBrand(2, "Dell",   true),
                    buildBrand(3, "Lenovo", true)
            );
            when(brandRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(brands);

            // Act
            List<BrandResponseDTO> result = brandService.getAllActive();

            // Assert
            assertEquals(3, result.size());
            assertEquals("Apple",  result.get(0).getName());
            assertEquals("Dell",   result.get(1).getName());
            assertEquals("Lenovo", result.get(2).getName());
            assertTrue(result.stream().allMatch(BrandResponseDTO::getIsActive));
        }

        @Test
        void should_returnEmptyList_when_noActiveBrandsExist() {
            // Arrange
            when(brandRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of());

            // Act
            List<BrandResponseDTO> result = brandService.getAllActive();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void should_mapAllFieldsCorrectly_when_convertingToDTO() {
            // Edge case: asegura que toDTO() no pierde campos al mapear
            Brand brand = buildBrand(42, "HP", true);
            when(brandRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(brand));

            // Act
            BrandResponseDTO dto = brandService.getAllActive().get(0);

            // Assert
            assertEquals(42,    dto.getId());
            assertEquals("HP",  dto.getName());
            assertTrue(dto.getIsActive());
        }
    }

    // =========================================================================
    // create()
    // =========================================================================
    @Nested
    class Create {

        @Test
        void should_returnCreatedDTO_when_nameIsUnique() {
            // Arrange
            BrandRequestDTO request = buildRequest("Samsung");
            Brand saved = buildBrand(10, "Samsung", true);

            when(brandRepository.existsByNameIgnoreCase("Samsung")).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenReturn(saved);

            // Act
            BrandResponseDTO result = brandService.create(request);

            // Assert
            assertNotNull(result);
            assertEquals(10,        result.getId());
            assertEquals("Samsung", result.getName());
            assertTrue(result.getIsActive());
        }

        @Test
        void should_throwDuplicateResourceException_when_nameAlreadyExists() {
            // Arrange
            BrandRequestDTO request = buildRequest("Apple");
            when(brandRepository.existsByNameIgnoreCase("Apple")).thenReturn(true);

            // Act & Assert
            assertThrows(
                    DuplicateResourceException.class,
                    () -> brandService.create(request)
            );
            verify(brandRepository, never()).save(any());
        }

        @Test
        void should_trimName_before_checkingDuplicateAndSaving() {
            // Edge case: "  Sony  " y "Sony" deben tratarse como el mismo nombre
            BrandRequestDTO request = buildRequest("  Sony  ");
            Brand saved = buildBrand(11, "Sony", true);

            when(brandRepository.existsByNameIgnoreCase("Sony")).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenReturn(saved);

            // Act
            BrandResponseDTO result = brandService.create(request);

            // Assert — el repositorio recibe "Sony", no "  Sony  "
            verify(brandRepository).existsByNameIgnoreCase("Sony");
            ArgumentCaptor<Brand> captor = ArgumentCaptor.forClass(Brand.class);
            verify(brandRepository).save(captor.capture());
            assertEquals("Sony", captor.getValue().getName());
            assertEquals("Sony", result.getName());
        }

        @Test
        void should_setIsActiveTrue_when_brandIsCreated() {
            // Edge case: el caller no debe poder crear una marca inactiva
            BrandRequestDTO request = buildRequest("LG");
            Brand saved = buildBrand(12, "LG", true);

            when(brandRepository.existsByNameIgnoreCase("LG")).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenReturn(saved);

            // Act
            brandService.create(request);

            // Assert
            ArgumentCaptor<Brand> captor = ArgumentCaptor.forClass(Brand.class);
            verify(brandRepository).save(captor.capture());
            assertTrue(captor.getValue().getIsActive(),
                    "Una marca recién creada debe estar activa");
        }

        @Test
        void should_throwDuplicateResourceException_when_nameMatchesCaseInsensitive() {
            // Edge case: "apple", "Apple" y "APPLE" son el mismo nombre
            BrandRequestDTO request = buildRequest("APPLE");
            when(brandRepository.existsByNameIgnoreCase("APPLE")).thenReturn(true);

            // Act & Assert
            assertThrows(
                    DuplicateResourceException.class,
                    () -> brandService.create(request)
            );
        }
    }

    // =========================================================================
    // update()
    // =========================================================================
    @Nested
    class Update {

        @Test
        void should_returnUpdatedDTO_when_brandExistsAndNameIsUnique() {
            // Arrange
            Brand existing = buildBrand(5, "Toshiba", true);
            BrandRequestDTO request = buildRequest("Toshiba Corp");
            Brand updated = buildBrand(5, "Toshiba Corp", true);

            when(brandRepository.findById(5)).thenReturn(Optional.of(existing));
            when(brandRepository.existsByNameIgnoreCase("Toshiba Corp")).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenReturn(updated);

            // Act
            BrandResponseDTO result = brandService.update(5, request);

            // Assert
            assertEquals(5,             result.getId());
            assertEquals("Toshiba Corp", result.getName());
        }

        @Test
        void should_throwResourceNotFoundException_when_brandDoesNotExist() {
            // Arrange
            BrandRequestDTO request = buildRequest("Fantasma");
            when(brandRepository.findById(99)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> brandService.update(99, request)
            );
            verify(brandRepository, never()).save(any());
        }

        @Test
        void should_throwDuplicateResourceException_when_newNameBelongsToAnotherBrand() {
            // Arrange
            Brand existing = buildBrand(5, "Toshiba", true);
            BrandRequestDTO request = buildRequest("Dell"); // nombre ya tomado por otra marca

            when(brandRepository.findById(5)).thenReturn(Optional.of(existing));
            when(brandRepository.existsByNameIgnoreCase("Dell")).thenReturn(true);

            // Act & Assert
            assertThrows(
                    DuplicateResourceException.class,
                    () -> brandService.update(5, request)
            );
            verify(brandRepository, never()).save(any());
        }

        @Test
        void should_skipDuplicateCheck_when_nameDidNotChange() {
            // Edge case clave: actualizar con el mismo nombre no debe lanzar excepción
            Brand existing = buildBrand(5, "Toshiba", true);
            BrandRequestDTO request = buildRequest("Toshiba"); // mismo nombre
            Brand saved = buildBrand(5, "Toshiba", true);

            when(brandRepository.findById(5)).thenReturn(Optional.of(existing));
            when(brandRepository.save(any(Brand.class))).thenReturn(saved);

            // Act — no debe lanzar excepción
            assertDoesNotThrow(() -> brandService.update(5, request));

            // Assert — existsByNameIgnoreCase NO debe llamarse cuando el nombre no cambia
            verify(brandRepository, never()).existsByNameIgnoreCase(anyString());
        }

        @Test
        void should_skipDuplicateCheck_when_nameChangesOnlyInCase() {
            // Edge case: "toshiba" → "TOSHIBA" — mismo nombre, distinto case
            Brand existing = buildBrand(5, "toshiba", true);
            BrandRequestDTO request = buildRequest("TOSHIBA");
            Brand saved = buildBrand(5, "TOSHIBA", true);

            when(brandRepository.findById(5)).thenReturn(Optional.of(existing));
            when(brandRepository.save(any(Brand.class))).thenReturn(saved);

            // Act
            assertDoesNotThrow(() -> brandService.update(5, request));

            // Assert — equalsIgnoreCase bloquea la verificación de duplicado
            verify(brandRepository, never()).existsByNameIgnoreCase(anyString());
        }

        @Test
        void should_trimName_before_updateAndDuplicateCheck() {
            // Edge case: espacios en el request deben ignorarse antes de comparar
            Brand existing = buildBrand(5, "Toshiba", true);
            BrandRequestDTO request = buildRequest("  Toshiba Corp  ");
            Brand saved = buildBrand(5, "Toshiba Corp", true);

            when(brandRepository.findById(5)).thenReturn(Optional.of(existing));
            when(brandRepository.existsByNameIgnoreCase("Toshiba Corp")).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenReturn(saved);

            // Act
            brandService.update(5, request);

            // Assert
            ArgumentCaptor<Brand> captor = ArgumentCaptor.forClass(Brand.class);
            verify(brandRepository).save(captor.capture());
            assertEquals("Toshiba Corp", captor.getValue().getName());
        }
    }

    // =========================================================================
    // delete()
    // =========================================================================
    @Nested
    class Delete {

        @Test
        void should_deactivateBrand_when_brandExistsAndHasNoLinkedAssets() {
            // Arrange
            Brand brand = buildBrand(7, "Epson", true);
            when(brandRepository.findById(7)).thenReturn(Optional.of(brand));
            when(assetRepository.existsByBrand(brand)).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenReturn(brand);

            // Act
            brandService.delete(7);

            // Assert — soft delete: isActive=false, no se elimina el registro
            ArgumentCaptor<Brand> captor = ArgumentCaptor.forClass(Brand.class);
            verify(brandRepository).save(captor.capture());
            assertFalse(captor.getValue().getIsActive(),
                    "El soft delete debe poner isActive=false, no borrar el registro");
        }

        @Test
        void should_throwResourceNotFoundException_when_brandDoesNotExist() {
            // Arrange
            when(brandRepository.findById(99)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> brandService.delete(99)
            );
            verify(assetRepository, never()).existsByBrand(any());
            verify(brandRepository, never()).save(any());
        }

        @Test
        void should_throwDuplicateResourceException_when_brandHasLinkedAssets() {
            // Arrange
            // Nota: DuplicateResourceException es semánticamente incorrecto aquí
            // (debería ser ResourceInUseException), pero refleja el comportamiento actual.
            Brand brand = buildBrand(7, "Epson", true);
            when(brandRepository.findById(7)).thenReturn(Optional.of(brand));
            when(assetRepository.existsByBrand(brand)).thenReturn(true);

            // Act & Assert
            assertThrows(
                    DuplicateResourceException.class,
                    () -> brandService.delete(7)
            );
            verify(brandRepository, never()).save(any());
        }

        @Test
        void should_checkLinkedAssetsWithCorrectBrandInstance() {
            // Edge case: assetRepository.existsByBrand() debe recibir la instancia
            // exacta del Brand encontrado, no una nueva instancia construida por el test
            Brand brand = buildBrand(7, "Epson", true);
            when(brandRepository.findById(7)).thenReturn(Optional.of(brand));
            when(assetRepository.existsByBrand(brand)).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenReturn(brand);

            // Act
            brandService.delete(7);

            // Assert — verifica que se pasó la misma referencia que devolvió el repositorio
            verify(assetRepository).existsByBrand(brand);
        }

        @Test
        void should_notDeleteRecord_when_performingSoftDelete() {
            // Edge case: delete() nunca debe llamar a deleteById() ni delete()
            Brand brand = buildBrand(7, "Epson", true);
            when(brandRepository.findById(7)).thenReturn(Optional.of(brand));
            when(assetRepository.existsByBrand(brand)).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenReturn(brand);

            // Act
            brandService.delete(7);

            // Assert
            verify(brandRepository, never()).deleteById(any());
            verify(brandRepository, never()).delete(any());
        }
    }
}