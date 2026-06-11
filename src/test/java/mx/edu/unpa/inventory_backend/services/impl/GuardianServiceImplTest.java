package mx.edu.unpa.inventory_backend.services.impl;

import mx.edu.unpa.inventory_backend.domains.Guardian;
import mx.edu.unpa.inventory_backend.domains.Location;
import mx.edu.unpa.inventory_backend.dtos.guardian.request.GuardianRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.guardian.response.GuardianResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.mappers.GuardianMapper;
import mx.edu.unpa.inventory_backend.repositories.GuardianRepository;
import mx.edu.unpa.inventory_backend.repositories.LocationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuardianServiceImpl Unit Tests")
class GuardianServiceImplTest {

    @Mock
    private GuardianRepository guardianRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private GuardianMapper guardianMapper;

    @InjectMocks
    private GuardianServiceImpl guardianService;

    // ── Fixtures reutilizables ─────────────────────────────────────────────────

    private GuardianRequestDTO buildRequest(String employeeNumber, Integer locationId) {
        return new GuardianRequestDTO(
                employeeNumber,
                "Juan Pérez López",
                "juan.perez@unpa.edu.mx",
                "9511234567",
                "Sistemas",
                locationId
        );
    }

    private Guardian buildGuardian(Long id, String employeeNumber) {
        Guardian g = new Guardian();
        g.setId(id);
        g.setEmployeeNumber(employeeNumber);
        g.setFullName("Juan Pérez López");
        g.setEmail("juan.perez@unpa.edu.mx");
        g.setPhone("9511234567");
        g.setDepartment("Sistemas");
        g.setIsActive(true);
        return g;
    }

    private GuardianResponseDTO buildResponseDTO(Long id, String employeeNumber, Integer locationId) {
        return new GuardianResponseDTO(
                id, employeeNumber, "Juan Pérez López",
                "juan.perez@unpa.edu.mx", "9511234567",
                "Sistemas", locationId, "Edificio A", true
        );
    }

    private Location buildLocation(Integer id) {
        Location loc = new Location();
        loc.setId(id);
        loc.setName("Edificio A");
        loc.setIsActive(true);
        return loc;
    }

    // ── create() ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("should_createGuardianWithLocation_when_requestIsValidAndLocationExists")
        void should_createGuardianWithLocation_when_requestIsValidAndLocationExists() {
            // Arrange
            GuardianRequestDTO request = buildRequest("EMP-001", 10);
            Guardian entity          = buildGuardian(null, "EMP-001");
            Guardian saved           = buildGuardian(1L, "EMP-001");
            GuardianResponseDTO expected = buildResponseDTO(1L, "EMP-001", 10);
            Location location        = buildLocation(10);

            when(guardianRepository.existsByEmployeeNumber("EMP-001")).thenReturn(false);
            when(guardianMapper.toEntity(request)).thenReturn(entity);
            when(locationRepository.findByIdAndIsActiveTrue(10)).thenReturn(Optional.of(location));
            when(guardianRepository.save(entity)).thenReturn(saved);
            when(guardianMapper.toDto(saved)).thenReturn(expected);

            // Act
            GuardianResponseDTO result = guardianService.create(request);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.id());
            assertEquals("EMP-001", result.employeeNumber());
            assertEquals(10, result.locationId());

            verify(guardianRepository).existsByEmployeeNumber("EMP-001");
            verify(guardianMapper).toEntity(request);
            verify(locationRepository).findByIdAndIsActiveTrue(10);
            verify(guardianRepository).save(entity);
            verify(guardianMapper).toDto(saved);
        }

        @Test
        @DisplayName("should_setIsActiveTrueOnNewGuardian_when_entityIsCreated")
        void should_setIsActiveTrueOnNewGuardian_when_entityIsCreated() {
            // Arrange
            GuardianRequestDTO request = buildRequest("EMP-002", null);
            Guardian entity = new Guardian(); // isActive empieza sin setear
            Guardian saved  = buildGuardian(2L, "EMP-002");

            when(guardianRepository.existsByEmployeeNumber("EMP-002")).thenReturn(false);
            when(guardianMapper.toEntity(request)).thenReturn(entity);
            when(guardianRepository.save(entity)).thenReturn(saved);
            when(guardianMapper.toDto(saved)).thenReturn(buildResponseDTO(2L, "EMP-002", null));

            // Act
            guardianService.create(request);

            // Assert — el servicio debe setear isActive = true antes de persistir
            assertTrue(entity.getIsActive());
        }

        @Test
        @DisplayName("should_createGuardianWithoutLocation_when_locationIdIsNull")
        void should_createGuardianWithoutLocation_when_locationIdIsNull() {
            // Arrange
            GuardianRequestDTO request = buildRequest("EMP-003", null);
            Guardian entity = buildGuardian(null, "EMP-003");
            Guardian saved  = buildGuardian(3L, "EMP-003");

            when(guardianRepository.existsByEmployeeNumber("EMP-003")).thenReturn(false);
            when(guardianMapper.toEntity(request)).thenReturn(entity);
            when(guardianRepository.save(entity)).thenReturn(saved);
            when(guardianMapper.toDto(saved)).thenReturn(buildResponseDTO(3L, "EMP-003", null));

            // Act
            guardianService.create(request);

            // Assert — no debe consultar locationRepository si locationId es null
            verifyNoInteractions(locationRepository);
            assertNull(entity.getLocation());
        }

        @Test
        @DisplayName("should_throwIllegalArgumentException_when_employeeNumberAlreadyExists")
        void should_throwIllegalArgumentException_when_employeeNumberAlreadyExists() {
            // Arrange
            GuardianRequestDTO request = buildRequest("EMP-DUP", null);
            when(guardianRepository.existsByEmployeeNumber("EMP-DUP")).thenReturn(true);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> guardianService.create(request)
            );

            assertTrue(ex.getMessage().contains("EMP-DUP"));
            verify(guardianRepository, never()).save(any());
            verifyNoInteractions(guardianMapper);
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_locationIdDoesNotExistOrIsInactive")
        void should_throwResourceNotFoundException_when_locationIdDoesNotExistOrIsInactive() {
            // Arrange
            GuardianRequestDTO request = buildRequest("EMP-004", 99);
            Guardian entity = buildGuardian(null, "EMP-004");

            when(guardianRepository.existsByEmployeeNumber("EMP-004")).thenReturn(false);
            when(guardianMapper.toEntity(request)).thenReturn(entity);
            when(locationRepository.findByIdAndIsActiveTrue(99)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> guardianService.create(request)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(guardianRepository, never()).save(any());
        }
    }

    // ── findById() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("should_returnResponseDTO_when_guardianExistsWithGivenId")
        void should_returnResponseDTO_when_guardianExistsWithGivenId() {
            // Arrange
            Guardian guardian = buildGuardian(1L, "EMP-001");
            GuardianResponseDTO expected = buildResponseDTO(1L, "EMP-001", null);

            when(guardianRepository.findById(1L)).thenReturn(Optional.of(guardian));
            when(guardianMapper.toDto(guardian)).thenReturn(expected);

            // Act
            GuardianResponseDTO result = guardianService.findById(1L);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.id());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_guardianDoesNotExist")
        void should_throwResourceNotFoundException_when_guardianDoesNotExist() {
            // Arrange
            when(guardianRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class, // <-- Actualizado
                    () -> guardianService.findById(999L)
            );

            // Opcional: Validar el mensaje de tu excepción
            assertTrue(exception.getMessage().contains("Resguardante no encontrado con id: 999"));
        }
    }

    // ── findAllActive() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAllActive()")
    class FindAllActiveTests {

        @Test
        @DisplayName("should_returnPageOfActiveguardians_when_activeGuardiansExist")
        void should_returnPageOfActiveGuardians_when_activeGuardiansExist() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Guardian g1 = buildGuardian(1L, "EMP-001");
            Guardian g2 = buildGuardian(2L, "EMP-002");
            Page<Guardian> guardianPage = new PageImpl<>(List.of(g1, g2), pageable, 2);

            GuardianResponseDTO dto1 = buildResponseDTO(1L, "EMP-001", null);
            GuardianResponseDTO dto2 = buildResponseDTO(2L, "EMP-002", null);

            when(guardianRepository.findByIsActiveTrue(pageable)).thenReturn(guardianPage);
            when(guardianMapper.toDto(g1)).thenReturn(dto1);
            when(guardianMapper.toDto(g2)).thenReturn(dto2);

            // Act
            Page<GuardianResponseDTO> result = guardianService.findAllActive(pageable);

            // Assert
            assertEquals(2, result.getTotalElements());
            assertEquals("EMP-001", result.getContent().get(0).employeeNumber());
            assertEquals("EMP-002", result.getContent().get(1).employeeNumber());
        }

        @Test
        @DisplayName("should_returnEmptyPage_when_noActiveGuardiansExist")
        void should_returnEmptyPage_when_noActiveGuardiansExist() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(guardianRepository.findByIsActiveTrue(pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            // Act
            Page<GuardianResponseDTO> result = guardianService.findAllActive(pageable);

            // Assert
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalElements());
        }
    }

    // ── search() ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("search()")
    class SearchTests {

        @Test
        @DisplayName("should_returnMatchingGuardians_when_queryMatchesActiveGuardians")
        void should_returnMatchingGuardians_when_queryMatchesActiveGuardians() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            String query = "Juan";
            Guardian guardian = buildGuardian(1L, "EMP-001");
            Page<Guardian> page = new PageImpl<>(List.of(guardian), pageable, 1);
            GuardianResponseDTO dto = buildResponseDTO(1L, "EMP-001", null);

            when(guardianRepository.searchActive(query, pageable)).thenReturn(page);
            when(guardianMapper.toDto(guardian)).thenReturn(dto);

            // Act
            Page<GuardianResponseDTO> result = guardianService.search(query, pageable);

            // Assert
            assertEquals(1, result.getTotalElements());
            assertEquals("EMP-001", result.getContent().get(0).employeeNumber());
        }

        @Test
        @DisplayName("should_returnEmptyPage_when_noGuardiansMatchQuery")
        void should_returnEmptyPage_when_noGuardiansMatchQuery() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(guardianRepository.searchActive("nonexistent", pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            // Act
            Page<GuardianResponseDTO> result = guardianService.search("nonexistent", pageable);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    // ── update() ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("should_updateGuardian_when_employeeNumberIsUnchanged")
        void should_updateGuardian_when_employeeNumberIsUnchanged() {
            // Arrange — mismo número de empleado, no debe validar unicidad contra repo
            GuardianRequestDTO request = buildRequest("EMP-001", null);
            Guardian existing = buildGuardian(1L, "EMP-001");
            Guardian saved    = buildGuardian(1L, "EMP-001");
            GuardianResponseDTO expected = buildResponseDTO(1L, "EMP-001", null);

            when(guardianRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(guardianRepository.save(existing)).thenReturn(saved);
            when(guardianMapper.toDto(saved)).thenReturn(expected);

            // Act
            GuardianResponseDTO result = guardianService.update(1L, request);

            // Assert
            assertNotNull(result);
            // No debe llamar existsByEmployeeNumber si el número no cambió
            verify(guardianRepository, never()).existsByEmployeeNumber(any());
        }

        @Test
        @DisplayName("should_updateGuardian_when_employeeNumberChangesAndNewNumberIsUnique")
        void should_updateGuardian_when_employeeNumberChangesAndNewNumberIsUnique() {
            // Arrange
            GuardianRequestDTO request = buildRequest("EMP-NEW", null);
            Guardian existing = buildGuardian(1L, "EMP-OLD");
            Guardian saved    = buildGuardian(1L, "EMP-NEW");
            GuardianResponseDTO expected = buildResponseDTO(1L, "EMP-NEW", null);

            when(guardianRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(guardianRepository.existsByEmployeeNumber("EMP-NEW")).thenReturn(false);
            when(guardianRepository.save(existing)).thenReturn(saved);
            when(guardianMapper.toDto(saved)).thenReturn(expected);

            // Act
            GuardianResponseDTO result = guardianService.update(1L, request);

            // Assert
            assertNotNull(result);
            verify(guardianRepository).existsByEmployeeNumber("EMP-NEW");
        }

        @Test
        @DisplayName("should_throwIllegalArgumentException_when_newEmployeeNumberAlreadyBelongsToAnotherGuardian")
        void should_throwIllegalArgumentException_when_newEmployeeNumberAlreadyBelongsToAnotherGuardian() {
            // Arrange
            GuardianRequestDTO request = buildRequest("EMP-TAKEN", null);
            Guardian existing = buildGuardian(1L, "EMP-OLD");

            when(guardianRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(guardianRepository.existsByEmployeeNumber("EMP-TAKEN")).thenReturn(true);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> guardianService.update(1L, request)
            );

            assertTrue(ex.getMessage().contains("EMP-TAKEN"));
            verify(guardianRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_clearLocation_when_locationIdIsNullInUpdateRequest")
        void should_clearLocation_when_locationIdIsNullInUpdateRequest() {
            // Arrange — locationId null debe limpiar la ubicación del resguardante
            GuardianRequestDTO request = buildRequest("EMP-001", null);
            Guardian existing = buildGuardian(1L, "EMP-001");
            existing.setLocation(buildLocation(10)); // tenía ubicación previa
            Guardian saved = buildGuardian(1L, "EMP-001");

            when(guardianRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(guardianRepository.save(existing)).thenReturn(saved);
            when(guardianMapper.toDto(saved)).thenReturn(buildResponseDTO(1L, "EMP-001", null));

            // Act
            guardianService.update(1L, request);

            // Assert — la ubicación debe haber sido limpiada
            assertNull(existing.getLocation());
            verifyNoInteractions(locationRepository);
        }

        @Test
        @DisplayName("should_updateLocation_when_newLocationIdIsProvided")
        void should_updateLocation_when_newLocationIdIsProvided() {
            // Arrange
            GuardianRequestDTO request = buildRequest("EMP-001", 20);
            Guardian existing = buildGuardian(1L, "EMP-001");
            Location newLocation = buildLocation(20);
            Guardian saved = buildGuardian(1L, "EMP-001");

            when(guardianRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(locationRepository.findByIdAndIsActiveTrue(20)).thenReturn(Optional.of(newLocation));
            when(guardianRepository.save(existing)).thenReturn(saved);
            when(guardianMapper.toDto(saved)).thenReturn(buildResponseDTO(1L, "EMP-001", 20));

            // Act
            guardianService.update(1L, request);

            // Assert
            assertEquals(newLocation, existing.getLocation());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_updatingNonExistentGuardian")
        void should_throwResourceNotFoundException_when_updatingNonExistentGuardian() {
            // Arrange
            GuardianRequestDTO request = new GuardianRequestDTO("EMP001", "John", null, null, null, null);
            when(guardianRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> guardianService.update(999L, request)
            );

            verify(guardianRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_newLocationIdDoesNotExistOrIsInactive")
        void should_throwResourceNotFoundException_when_newLocationIdDoesNotExistOrIsInactive() {
            // Arrange
            GuardianRequestDTO request = buildRequest("EMP-001", 99);
            Guardian existing = buildGuardian(1L, "EMP-001");

            when(guardianRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(locationRepository.findByIdAndIsActiveTrue(99)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> guardianService.update(1L, request)
            );

            assertTrue(ex.getMessage().contains("99"));
            verify(guardianRepository, never()).save(any());
        }
    }

    // ── deactivate() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deactivate()")
    class DeactivateTests {

        @Test
        @DisplayName("should_setIsActiveFalse_when_guardianExistsAndIsCurrentlyActive")
        void should_setIsActiveFalse_when_guardianExistsAndIsCurrentlyActive() {
            // Arrange
            Guardian guardian = buildGuardian(1L, "EMP-001");
            guardian.setIsActive(true);

            when(guardianRepository.findById(1L)).thenReturn(Optional.of(guardian));
            when(guardianRepository.save(guardian)).thenReturn(guardian);

            // Act
            guardianService.deactivate(1L);

            // Assert
            assertFalse(guardian.getIsActive());
            verify(guardianRepository).save(guardian);
        }

        @Test
        @DisplayName("should_saveWithIsActiveFalse_when_deactivatingAlreadyInactiveGuardian")
        void should_saveWithIsActiveFalse_when_deactivatingAlreadyInactiveGuardian() {
            // Edge case: idempotencia — desactivar un guardián ya inactivo no debe lanzar error
            Guardian guardian = buildGuardian(1L, "EMP-001");
            guardian.setIsActive(false);

            when(guardianRepository.findById(1L)).thenReturn(Optional.of(guardian));
            when(guardianRepository.save(guardian)).thenReturn(guardian);

            // Act
            assertDoesNotThrow(() -> guardianService.deactivate(1L));

            // Assert
            assertFalse(guardian.getIsActive());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_deactivatingNonExistentGuardian")
        void should_throwResourceNotFoundException_when_deactivatingNonExistentGuardian() {
            // Arrange
            when(guardianRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    ResourceNotFoundException.class, // <-- Actualizado
                    () -> guardianService.deactivate(999L)
            );

            verify(guardianRepository, never()).save(any());
        }
    }
}