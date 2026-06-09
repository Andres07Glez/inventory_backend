package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Location;
import mx.edu.unpa.inventory_backend.enums.Campus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LocationRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private LocationRepository locationRepository;

    // =========================================================================
    // findByIdAndIsActiveTrue
    // =========================================================================

    @Test
    void should_returnLocation_when_locationIsActive() {
        // Arrange
        Location active = persistLocation("Sala A", "Edificio Norte", Campus.LOMA_BONITA, true);
        entityManager.clear();

        // Act
        Optional<Location> result = locationRepository.findByIdAndIsActiveTrue(active.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Sala A");
    }

    @Test
    void should_returnEmpty_when_locationIsInactive() {
        // Arrange
        Location inactive = persistLocation("Sala B", "Edificio Sur", Campus.TUXTEPEC, false);
        entityManager.clear();

        // Act
        Optional<Location> result = locationRepository.findByIdAndIsActiveTrue(inactive.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_idDoesNotExist() {
        // Act
        Optional<Location> result = locationRepository.findByIdAndIsActiveTrue(999);

        // Assert
        assertThat(result).isEmpty();
    }

    // =========================================================================
    // findByIsActiveTrue (paginación)
    // =========================================================================

    @Test
    void should_returnOnlyActiveLocations_when_mixedLocationsExist() {
        // Arrange
        persistLocation("Activa 1", null, null, true);
        persistLocation("Activa 2", null, null, true);
        persistLocation("Inactiva 1", null, null, false);
        entityManager.clear();

        // Act
        Page<Location> result = locationRepository.findByIsActiveTrue(PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Location::getName)
                .containsExactlyInAnyOrder("Activa 1", "Activa 2");
    }

    @Test
    void should_returnEmptyPage_when_noActiveLocationsExist() {
        // Arrange
        persistLocation("Inactiva", null, Campus.TUXTEPEC, false);
        entityManager.clear();

        // Act
        Page<Location> result = locationRepository.findByIsActiveTrue(PageRequest.of(0, 10));

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_respectPagination_when_multipleActiveLocationsExist() {
        // Arrange
        for (int i = 1; i <= 5; i++) {
            persistLocation("Ubicacion " + i, null, null, true);
        }
        entityManager.clear();

        // Act
        Page<Location> page = locationRepository.findByIsActiveTrue(PageRequest.of(0, 3));

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    // =========================================================================
    // existsByNameAndCampus
    // =========================================================================

    @Test
    void should_returnTrue_when_locationWithSameNameAndCampusExists() {
        // Arrange
        persistLocation("Laboratorio 1", "Edificio A", Campus.LOMA_BONITA, true);
        entityManager.clear();

        // Act
        boolean exists = locationRepository.existsByNameAndCampus("Laboratorio 1", Campus.LOMA_BONITA);

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void should_returnFalse_when_sameNameButDifferentCampus() {
        // Arrange
        persistLocation("Laboratorio 1", "Edificio A", Campus.LOMA_BONITA, true);
        entityManager.clear();

        // Act
        boolean exists = locationRepository.existsByNameAndCampus("Laboratorio 1", Campus.TUXTEPEC);

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void should_returnFalse_when_sameCampusButDifferentName() {
        // Arrange
        persistLocation("Laboratorio 1", "Edificio A", Campus.LOMA_BONITA, true);
        entityManager.clear();

        // Act
        boolean exists = locationRepository.existsByNameAndCampus("Laboratorio X", Campus.LOMA_BONITA);

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void should_returnTrue_when_inactiveLocationMatchesNameAndCampus() {
        // Edge case: existsByNameAndCampus NO filtra por isActive.
        // Documenta el comportamiento actual: un nombre puede estar "ocupado"
        // aunque la ubicación esté inactiva. Revisar si la regla de negocio
        // requiere filtrar por isActive en el service layer.
        persistLocation("Sala Inactiva", null, Campus.TUXTEPEC, false);
        entityManager.clear();

        // Act
        boolean exists = locationRepository.existsByNameAndCampus("Sala Inactiva", Campus.TUXTEPEC);

        // Assert
        assertThat(exists).isTrue();
    }

    // =========================================================================
    // searchActive (JPQL @Query) ← objetivo principal
    // =========================================================================

    @Test
    void should_findByName_when_queryMatchesPartialName() {
        // Arrange
        persistLocation("Laboratorio de Computo", "Edificio A", Campus.LOMA_BONITA, true);
        persistLocation("Sala de Reuniones", "Edificio B", Campus.TUXTEPEC, true);
        entityManager.clear();

        // Act
        Page<Location> result = locationRepository.searchActive("Laboratorio", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Laboratorio de Computo");
    }

    @Test
    void should_findByBuilding_when_queryMatchesPartialBuilding() {
        // Arrange
        persistLocation("Oficina A", "Torre Administrativa", Campus.LOMA_BONITA, true);
        persistLocation("Oficina B", "Edificio Central", Campus.TUXTEPEC, true);
        entityManager.clear();

        // Act
        Page<Location> result = locationRepository.searchActive("Torre", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getBuilding()).isEqualTo("Torre Administrativa");
    }

    @Test
    void should_findByNameOrBuilding_when_queryMatchesBothFields() {
        // Arrange — "norte" aparece en el name de uno y en el building del otro
        persistLocation("Sala Norte",        "Edificio A",     Campus.LOMA_BONITA, true);
        persistLocation("Sala de Sistemas",  "Torre del Norte", Campus.TUXTEPEC,   true);
        entityManager.clear();

        // Act
        Page<Location> result = locationRepository.searchActive("norte", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Location::getName)
                .containsExactlyInAnyOrder("Sala Norte", "Sala de Sistemas");
    }

    @Test
    void should_beCaseInsensitive_when_queryHasDifferentCase() {
        // Arrange
        persistLocation("Laboratorio de Fisica", "Edificio C", Campus.LOMA_BONITA, true);
        entityManager.clear();

        // Act
        Page<Location> result = locationRepository.searchActive("LABORATORIO", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void should_excludeInactiveLocations_when_searchingByName() {
        // Arrange
        persistLocation("Laboratorio Activo",   "Edificio A", Campus.LOMA_BONITA, true);
        persistLocation("Laboratorio Inactivo", "Edificio B", Campus.TUXTEPEC,    false);
        entityManager.clear();

        // Act
        Page<Location> result = locationRepository.searchActive("Laboratorio", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Laboratorio Activo");
    }

    @Test
    void should_returnEmpty_when_queryMatchesNothingActive() {
        // Arrange
        persistLocation("Sala de Juntas", "Edificio A", Campus.LOMA_BONITA, true);
        entityManager.clear();

        // Act
        Page<Location> result = locationRepository.searchActive("xyz_inexistente", PageRequest.of(0, 10));

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_queryMatchesBuildingOfInactiveLocation() {
        // Edge case: building coincide pero la location está inactiva
        persistLocation("Oficina Cerrada", "Torre Fantasma", Campus.TUXTEPEC, false);
        entityManager.clear();

        // Act
        Page<Location> result = locationRepository.searchActive("Fantasma", PageRequest.of(0, 10));

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_handleNullBuilding_when_locationHasNoBuilding() {
        // Edge case: building es null — el LIKE sobre null no debe lanzar excepción
        // ni devolver resultados fantasma; Hibernate evalúa LIKE(null, ?) como false.
        persistLocation("Sala Exterior", null, Campus.LOMA_BONITA, true);
        entityManager.clear();

        // Act
        Page<Location> byName     = locationRepository.searchActive("Exterior", PageRequest.of(0, 10));
        Page<Location> byBuilding = locationRepository.searchActive("Torre",    PageRequest.of(0, 10));

        // Assert
        assertThat(byName.getTotalElements()).isEqualTo(1);
        assertThat(byBuilding).isEmpty();
    }

    // =========================================================================
    // findByIsActiveTrueAndCampus
    // =========================================================================

    @Test
    void should_returnOnlyLocationsFromGivenCampus_when_multiCampusDataExists() {
        // Arrange
        persistLocation("Sala 1", null, Campus.LOMA_BONITA, true);
        persistLocation("Sala 2", null, Campus.LOMA_BONITA, true);
        persistLocation("Sala 3", null, Campus.TUXTEPEC,    true);
        entityManager.clear();

        // Act
        Page<Location> result = locationRepository.findByIsActiveTrueAndCampus(
                Campus.LOMA_BONITA, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Location::getCampus)
                .containsOnly(Campus.LOMA_BONITA);
    }

    @Test
    void should_excludeInactiveLocations_when_filteringByCampus() {
        // Arrange
        persistLocation("Activa LB",   null, Campus.LOMA_BONITA, true);
        persistLocation("Inactiva LB", null, Campus.LOMA_BONITA, false);
        entityManager.clear();

        // Act
        Page<Location> result = locationRepository.findByIsActiveTrueAndCampus(
                Campus.LOMA_BONITA, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Activa LB");
    }

    @Test
    void should_returnEmpty_when_noCampusMatches() {
        // Arrange — solo hay ubicaciones en LOMA_BONITA
        persistLocation("Sala LB", null, Campus.LOMA_BONITA, true);
        entityManager.clear();

        // Act
        Page<Location> result = locationRepository.findByIsActiveTrueAndCampus(
                Campus.TUXTEPEC, PageRequest.of(0, 10));

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_locationHasNullCampusAndFilteringBySpecificCampus() {
        // Edge case: una location sin campus asignado NO debe aparecer al filtrar
        // por campus específico — H2 trata campus IS NULL como no igual a LOMA_BONITA.
        persistLocation("Sin campus", null, null, true);
        entityManager.clear();

        // Act
        Page<Location> result = locationRepository.findByIsActiveTrueAndCampus(
                Campus.LOMA_BONITA, PageRequest.of(0, 10));

        // Assert
        assertThat(result).isEmpty();
    }

    // =========================================================================
    // Helper de construcción — extiende buildLocation(name) de BaseRepositoryTest
    // =========================================================================

    private Location persistLocation(String name, String building, Campus campus, boolean isActive) {
        Location l = buildLocation(name);   // reutiliza el helper de la clase base
        l.setBuilding(building);
        l.setCampus(campus);
        l.setIsActive(isActive);
        return entityManager.persistAndFlush(l);
    }
}