package mx.edu.unpa.inventory_backend.services.impl;

import jakarta.persistence.EntityNotFoundException;
import mx.edu.unpa.inventory_backend.domains.Location;
import mx.edu.unpa.inventory_backend.dtos.location.request.LocationRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.location.response.LocationResponseDTO;
import mx.edu.unpa.inventory_backend.enums.Campus;
import mx.edu.unpa.inventory_backend.mappers.LocationMapper;
import mx.edu.unpa.inventory_backend.repositories.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para LocationServiceImpl.
 *
 * LocationMapper usa componentModel = "spring", por lo que NO se puede
 * instanciar con Mappers.getMapper() en un test puro. Se mockea con @Mock
 * y se stubbea explícitamente en cada test que lo necesite.
 */
@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    @Mock private LocationRepository locationRepository;
    @Mock private LocationMapper      locationMapper;

    @InjectMocks
    private LocationServiceImpl locationService;

    // ── Fixtures ──────────────────────────────────────────────

    private Location              location;
    private LocationResponseDTO   responseDTO;
    private LocationRequestDTO    validRequest;

    @BeforeEach
    void setUp() {
        location = new Location();
        location.setId(1);
        location.setName("Sala de Cómputo A");
        location.setBuilding("Edificio Principal");
        location.setCampus(Campus.LOMA_BONITA);
        location.setDescription("Sala principal de cómputo");
        location.setIsActive(true);

        responseDTO = new LocationResponseDTO(
                1,
                "Sala de Cómputo A",
                "Edificio Principal",
                Campus.LOMA_BONITA,
                "Sala principal de cómputo",
                true
        );

        validRequest = new LocationRequestDTO(
                "Sala de Cómputo A",
                "Edificio Principal",
                Campus.LOMA_BONITA,
                "Sala principal de cómputo"
        );
    }

    // ════════════════════════════════════════════════════════════
    // create
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should_returnCreatedDTO_when_requestIsValid")
        void should_returnCreatedDTO_when_requestIsValid() {
            // Arrange
            when(locationRepository.existsByNameAndCampus("Sala de Cómputo A", Campus.LOMA_BONITA))
                    .thenReturn(false);
            when(locationMapper.toEntity(validRequest)).thenReturn(location);
            when(locationRepository.save(location)).thenReturn(location);
            when(locationMapper.toDto(location)).thenReturn(responseDTO);

            // Act
            LocationResponseDTO result = locationService.create(validRequest);

            // Assert
            assertNotNull(result);
            assertEquals("Sala de Cómputo A", result.name());
            assertEquals(Campus.LOMA_BONITA, result.campus());
            verify(locationRepository).save(location);
        }

        @Test
        @DisplayName("should_setIsActiveTrue_when_creatingNewLocation")
        void should_setIsActiveTrue_when_creatingNewLocation() {
            // Arrange — el mapper devuelve una location sin isActive asignado
            Location entityFromMapper = new Location();
            entityFromMapper.setName("Sala de Cómputo A");
            entityFromMapper.setCampus(Campus.LOMA_BONITA);

            when(locationRepository.existsByNameAndCampus(any(), any())).thenReturn(false);
            when(locationMapper.toEntity(validRequest)).thenReturn(entityFromMapper);
            when(locationRepository.save(any())).thenReturn(location);
            when(locationMapper.toDto(any())).thenReturn(responseDTO);

            // Act
            locationService.create(validRequest);

            // Assert — el service debe forzar isActive = true antes de persistir
            verify(locationRepository).save(argThat(saved -> Boolean.TRUE.equals(saved.getIsActive())));
        }

        @Test
        @DisplayName("should_throwIllegalArgumentException_when_nameAndCampusAlreadyExist")
        void should_throwIllegalArgumentException_when_nameAndCampusAlreadyExist() {
            // Arrange
            when(locationRepository.existsByNameAndCampus("Sala de Cómputo A", Campus.LOMA_BONITA))
                    .thenReturn(true);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> locationService.create(validRequest)
            );
            assertTrue(ex.getMessage().contains("Sala de Cómputo A"));
            assertTrue(ex.getMessage().contains("LOMA_BONITA"));
            verify(locationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_allowCreate_when_sameNameExistsOnDifferentCampus")
        void should_allowCreate_when_sameNameExistsOnDifferentCampus() {
            // Misma sala, campus distinto → no es duplicado
            LocationRequestDTO tuxtepecRequest = new LocationRequestDTO(
                    "Sala de Cómputo A", "Edificio Norte", Campus.TUXTEPEC, null
            );
            when(locationRepository.existsByNameAndCampus("Sala de Cómputo A", Campus.TUXTEPEC))
                    .thenReturn(false);
            when(locationMapper.toEntity(tuxtepecRequest)).thenReturn(location);
            when(locationRepository.save(any())).thenReturn(location);
            when(locationMapper.toDto(any())).thenReturn(responseDTO);

            assertDoesNotThrow(() -> locationService.create(tuxtepecRequest));
            verify(locationRepository).save(any());
        }
    }

    // ════════════════════════════════════════════════════════════
    // findAllActive
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAllActive")
    class FindAllActive {

        @Test
        @DisplayName("should_returnPageOfActiveDTOs_when_activeLocationsExist")
        void should_returnPageOfActiveDTOs_when_activeLocationsExist() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Location> page = new PageImpl<>(List.of(location), pageable, 1);
            when(locationRepository.findByIsActiveTrue(pageable)).thenReturn(page);
            when(locationMapper.toDto(location)).thenReturn(responseDTO);

            // Act
            Page<LocationResponseDTO> result = locationService.findAllActive(pageable);

            // Assert
            assertEquals(1, result.getTotalElements());
            assertEquals("Sala de Cómputo A", result.getContent().get(0).name());
            verify(locationRepository).findByIsActiveTrue(pageable);
        }

        @Test
        @DisplayName("should_returnEmptyPage_when_noActiveLocationsExist")
        void should_returnEmptyPage_when_noActiveLocationsExist() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(locationRepository.findByIsActiveTrue(pageable))
                    .thenReturn(Page.empty(pageable));

            // Act
            Page<LocationResponseDTO> result = locationService.findAllActive(pageable);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    // ════════════════════════════════════════════════════════════
    // findByCampus
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByCampus")
    class FindByCampus {

        @Test
        @DisplayName("should_returnLocationsFilteredByCampus_when_campusMatches")
        void should_returnLocationsFilteredByCampus_when_campusMatches() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Location> page = new PageImpl<>(List.of(location), pageable, 1);
            when(locationRepository.findByIsActiveTrueAndCampus(Campus.LOMA_BONITA, pageable))
                    .thenReturn(page);
            when(locationMapper.toDto(location)).thenReturn(responseDTO);

            // Act
            Page<LocationResponseDTO> result = locationService.findByCampus(Campus.LOMA_BONITA, pageable);

            // Assert
            assertEquals(1, result.getTotalElements());
            assertEquals(Campus.LOMA_BONITA, result.getContent().get(0).campus());
            verify(locationRepository).findByIsActiveTrueAndCampus(Campus.LOMA_BONITA, pageable);
        }

        @Test
        @DisplayName("should_returnEmptyPage_when_noLocationsMatchCampus")
        void should_returnEmptyPage_when_noLocationsMatchCampus() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(locationRepository.findByIsActiveTrueAndCampus(Campus.TUXTEPEC, pageable))
                    .thenReturn(Page.empty(pageable));

            // Act
            Page<LocationResponseDTO> result = locationService.findByCampus(Campus.TUXTEPEC, pageable);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    // ════════════════════════════════════════════════════════════
    // search
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("should_returnMatchingLocations_when_queryMatchesNameOrBuilding")
        void should_returnMatchingLocations_when_queryMatchesNameOrBuilding() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Location> page = new PageImpl<>(List.of(location), pageable, 1);
            when(locationRepository.searchActive("Cómputo", pageable)).thenReturn(page);
            when(locationMapper.toDto(location)).thenReturn(responseDTO);

            // Act
            Page<LocationResponseDTO> result = locationService.search("Cómputo", pageable);

            // Assert
            assertEquals(1, result.getTotalElements());
            verify(locationRepository).searchActive("Cómputo", pageable);
        }

        @Test
        @DisplayName("should_returnEmptyPage_when_queryMatchesNothing")
        void should_returnEmptyPage_when_queryMatchesNothing() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(locationRepository.searchActive("XYZ_INEXISTENTE", pageable))
                    .thenReturn(Page.empty(pageable));

            // Act
            Page<LocationResponseDTO> result = locationService.search("XYZ_INEXISTENTE", pageable);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    // ════════════════════════════════════════════════════════════
    // findById
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should_returnDTO_when_locationExists")
        void should_returnDTO_when_locationExists() {
            // Arrange
            when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
            when(locationMapper.toDto(location)).thenReturn(responseDTO);

            // Act
            LocationResponseDTO result = locationService.findById(1L);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.id());
            assertEquals("Sala de Cómputo A", result.name());
        }

        @Test
        @DisplayName("should_throwEntityNotFoundException_when_locationNotFound")
        void should_throwEntityNotFoundException_when_locationNotFound() {
            // Arrange
            when(locationRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            EntityNotFoundException ex = assertThrows(
                    EntityNotFoundException.class,
                    () -> locationService.findById(99L)
            );
            assertTrue(ex.getMessage().contains("99"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // update
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should_returnUpdatedDTO_when_requestIsValid")
        void should_returnUpdatedDTO_when_requestIsValid() {
            // Arrange
            LocationRequestDTO updateRequest = new LocationRequestDTO(
                    "Sala de Cómputo B", "Edificio Sur", Campus.LOMA_BONITA, "Descripción nueva"
            );
            LocationResponseDTO updatedDTO = new LocationResponseDTO(
                    1, "Sala de Cómputo B", "Edificio Sur", Campus.LOMA_BONITA, "Descripción nueva", true
            );

            when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
            when(locationRepository.existsByNameAndCampus("Sala de Cómputo B", Campus.LOMA_BONITA))
                    .thenReturn(false);
            when(locationRepository.save(location)).thenReturn(location);
            when(locationMapper.toDto(location)).thenReturn(updatedDTO);

            // Act
            LocationResponseDTO result = locationService.update(1L, updateRequest);

            // Assert
            assertEquals("Sala de Cómputo B", result.name());
            verify(locationMapper).updateEntityFromDto(updateRequest, location);
            verify(locationRepository).save(location);
        }

        @Test
        @DisplayName("should_skipDuplicateCheck_when_nameAndCampusAreUnchanged")
        void should_skipDuplicateCheck_when_nameAndCampusAreUnchanged() {
            // El request trae exactamente el mismo nombre y campus que ya tiene la entidad
            when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
            when(locationRepository.save(location)).thenReturn(location);
            when(locationMapper.toDto(location)).thenReturn(responseDTO);

            locationService.update(1L, validRequest);

            // El check existsByNameAndCampus no debe invocarse
            verify(locationRepository, never()).existsByNameAndCampus(any(), any());
        }

        @Test
        @DisplayName("should_checkDuplicate_when_onlyNameChanges")
        void should_checkDuplicate_when_onlyNameChanges() {
            // Arrange — solo cambia el nombre, el campus permanece igual
            LocationRequestDTO nameChangedRequest = new LocationRequestDTO(
                    "Sala Nueva", "Edificio Principal", Campus.LOMA_BONITA, null
            );
            when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
            when(locationRepository.existsByNameAndCampus("Sala Nueva", Campus.LOMA_BONITA))
                    .thenReturn(false);
            when(locationRepository.save(location)).thenReturn(location);
            when(locationMapper.toDto(location)).thenReturn(responseDTO);

            // Act
            locationService.update(1L, nameChangedRequest);

            // Assert
            verify(locationRepository).existsByNameAndCampus("Sala Nueva", Campus.LOMA_BONITA);
        }

        @Test
        @DisplayName("should_checkDuplicate_when_onlyCampusChanges")
        void should_checkDuplicate_when_onlyCampusChanges() {
            // Arrange — solo cambia el campus
            LocationRequestDTO campusChangedRequest = new LocationRequestDTO(
                    "Sala de Cómputo A", "Edificio Principal", Campus.TUXTEPEC, null
            );
            when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
            when(locationRepository.existsByNameAndCampus("Sala de Cómputo A", Campus.TUXTEPEC))
                    .thenReturn(false);
            when(locationRepository.save(location)).thenReturn(location);
            when(locationMapper.toDto(location)).thenReturn(responseDTO);

            // Act
            locationService.update(1L, campusChangedRequest);

            // Assert
            verify(locationRepository).existsByNameAndCampus("Sala de Cómputo A", Campus.TUXTEPEC);
        }

        @Test
        @DisplayName("should_throwIllegalArgumentException_when_updatedNameAndCampusAlreadyExist")
        void should_throwIllegalArgumentException_when_updatedNameAndCampusAlreadyExist() {
            // Arrange
            LocationRequestDTO conflictRequest = new LocationRequestDTO(
                    "Sala Ocupada", "Edificio Norte", Campus.LOMA_BONITA, null
            );
            when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
            when(locationRepository.existsByNameAndCampus("Sala Ocupada", Campus.LOMA_BONITA))
                    .thenReturn(true);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> locationService.update(1L, conflictRequest)
            );
            assertTrue(ex.getMessage().contains("Sala Ocupada"));
            verify(locationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_throwEntityNotFoundException_when_locationToUpdateNotFound")
        void should_throwEntityNotFoundException_when_locationToUpdateNotFound() {
            when(locationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> locationService.update(99L, validRequest)
            );
            verify(locationRepository, never()).save(any());
        }
    }

    // ════════════════════════════════════════════════════════════
    // deactivate
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deactivate")
    class Deactivate {

        @Test
        @DisplayName("should_setIsActiveFalse_when_locationExists")
        void should_setIsActiveFalse_when_locationExists() {
            // Arrange
            when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
            when(locationRepository.save(any())).thenReturn(location);

            // Act
            locationService.deactivate(1L);

            // Assert
            verify(locationRepository).save(argThat(saved -> Boolean.FALSE.equals(saved.getIsActive())));
        }

        @Test
        @DisplayName("should_persistDeactivatedLocation_when_deactivationSucceeds")
        void should_persistDeactivatedLocation_when_deactivationSucceeds() {
            when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
            when(locationRepository.save(location)).thenReturn(location);

            locationService.deactivate(1L);

            verify(locationRepository, times(1)).save(location);
        }

        @Test
        @DisplayName("should_throwEntityNotFoundException_when_locationToDeactivateNotFound")
        void should_throwEntityNotFoundException_when_locationToDeactivateNotFound() {
            when(locationRepository.findById(99L)).thenReturn(Optional.empty());

            EntityNotFoundException ex = assertThrows(
                    EntityNotFoundException.class,
                    () -> locationService.deactivate(99L)
            );
            assertTrue(ex.getMessage().contains("99"));
            verify(locationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_notThrow_when_locationWasAlreadyInactive")
        void should_notThrow_when_locationWasAlreadyInactive() {
            // Una ubicación ya inactiva puede volver a recibir deactivate sin error
            location.setIsActive(false);
            when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
            when(locationRepository.save(any())).thenReturn(location);

            assertDoesNotThrow(() -> locationService.deactivate(1L));
            verify(locationRepository).save(argThat(saved -> Boolean.FALSE.equals(saved.getIsActive())));
        }
    }
}