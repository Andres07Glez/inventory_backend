package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.*;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetSearchResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetSearchResultDTO;
import mx.edu.unpa.inventory_backend.dtos.dashboard.response.LocationStatDTO;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


class AssetRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired private AssetRepository   repository;


    // =========================================================
    //  findByBarcodeWithDetails
    // =========================================================

    @Test
    void should_returnAssetWithCategory_when_barcodeExists() {
        Asset asset = buildAsset("INV-001");
        asset.setBarcode("BC-001");
        entityManager.persistAndFlush(asset);
        entityManager.clear(); // fuerza recarga para validar JOIN FETCH

        Optional<Asset> result = repository.findByBarcodeWithDetails("BC-001");

        assertThat(result).isPresent();
        assertThat(result.get().getBarcode()).isEqualTo("BC-001");
        assertThat(result.get().getCategory()).isNotNull();
    }

    @Test
    void should_returnEmpty_when_barcodeNotFound() {
        assertThat(repository.findByBarcodeWithDetails("NONEXISTENT")).isEmpty();
    }

    // =========================================================
    //  findByInventoryNumberWithDetails
    // =========================================================

    @Test
    void should_returnAssetWithCategory_when_inventoryNumberExists() {
        entityManager.persistAndFlush(buildAsset("INV-2024-001"));
        entityManager.clear();

        Optional<Asset> result = repository.findByInventoryNumberWithDetails("INV-2024-001");

        assertThat(result).isPresent();
        assertThat(result.get().getInventoryNumber()).isEqualTo("INV-2024-001");
        assertThat(result.get().getCategory()).isNotNull();
    }

    @Test
    void should_returnEmpty_when_inventoryNumberNotFound() {
        assertThat(repository.findByInventoryNumberWithDetails("NO-EXISTE")).isEmpty();
    }

    // =========================================================
    //  findByIdWithDetails
    // =========================================================

    @Test
    void should_returnAssetWithCategoryAndLocation_when_idExists() {
        Location location = entityManager.persistAndFlush(buildLocation("Sala A"));
        Asset asset = buildAsset("INV-001");
        asset.setLocation(location);
        Asset saved = entityManager.persistAndFlush(asset);
        entityManager.clear();

        Optional<Asset> result = repository.findByIdWithDetails(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getCategory()).isNotNull();  // JOIN FETCH
        assertThat(result.get().getLocation()).isNotNull();  // LEFT JOIN FETCH
    }

    @Test
    void should_returnEmpty_when_idNotFound() {
        assertThat(repository.findByIdWithDetails(999L)).isEmpty();
    }

    // =========================================================
    //  findByFilters (@EntityGraph)
    // =========================================================

    @Test
    void should_returnAllAssets_when_bothFiltersAreNull() {
        entityManager.persistAndFlush(buildAssetWithStatus("INV-001", ConditionStatus.GOOD,  LifecycleStatus.AVAILABLE));
        entityManager.persistAndFlush(buildAssetWithStatus("INV-002", ConditionStatus.BAD,   LifecycleStatus.ASSIGNED));

        Page<Asset> result = repository.findByFilters(null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void should_filterByConditionOnly_when_lifecycleIsNull() {
        entityManager.persistAndFlush(buildAssetWithStatus("INV-001", ConditionStatus.GOOD, LifecycleStatus.AVAILABLE));
        entityManager.persistAndFlush(buildAssetWithStatus("INV-002", ConditionStatus.BAD,  LifecycleStatus.AVAILABLE));

        Page<Asset> result = repository.findByFilters(ConditionStatus.GOOD, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getConditionStatus()).isEqualTo(ConditionStatus.GOOD);
    }

    @Test
    void should_filterByLifecycleOnly_when_conditionIsNull() {
        entityManager.persistAndFlush(buildAssetWithStatus("INV-001", ConditionStatus.GOOD, LifecycleStatus.ASSIGNED));
        entityManager.persistAndFlush(buildAssetWithStatus("INV-002", ConditionStatus.GOOD, LifecycleStatus.AVAILABLE));

        Page<Asset> result = repository.findByFilters(null, LifecycleStatus.ASSIGNED, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getLifecycleStatus()).isEqualTo(LifecycleStatus.ASSIGNED);
    }

    @Test
    void should_filterByBothFilters_when_conditionAndLifecycleAreProvided() {
        entityManager.persistAndFlush(buildAssetWithStatus("INV-001", ConditionStatus.GOOD, LifecycleStatus.ASSIGNED));
        entityManager.persistAndFlush(buildAssetWithStatus("INV-002", ConditionStatus.BAD,  LifecycleStatus.ASSIGNED));
        entityManager.persistAndFlush(buildAssetWithStatus("INV-003", ConditionStatus.GOOD, LifecycleStatus.AVAILABLE));

        Page<Asset> result = repository.findByFilters(
                ConditionStatus.GOOD, LifecycleStatus.ASSIGNED, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // =========================================================
    //  findFiltered (rangos de fecha + params opcionales)
    // =========================================================

    @Test
    void should_returnAllAssets_when_allNullableParamsAreNull() {
        entityManager.persistAndFlush(buildAssetWithEntryDate("INV-001", LocalDate.now().minusDays(10)));
        entityManager.persistAndFlush(buildAssetWithEntryDate("INV-002", LocalDate.now()));

        Page<Asset> result = repository.findFiltered(null, null, null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void should_filterByDateRange_when_startAndEndDateAreProvided() {
        entityManager.persistAndFlush(buildAssetWithEntryDate("INV-001", LocalDate.now().minusDays(15)));
        entityManager.persistAndFlush(buildAssetWithEntryDate("INV-002", LocalDate.now()));  // fuera del rango

        Page<Asset> result = repository.findFiltered(
                null, null,
                LocalDate.now().minusDays(20),
                LocalDate.now().minusDays(5),
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getInventoryNumber()).isEqualTo("INV-001");
    }

    @Test
    void should_filterByStartDateOnly_when_endDateIsNull() {
        entityManager.persistAndFlush(buildAssetWithEntryDate("INV-001", LocalDate.now()));               // después del corte
        entityManager.persistAndFlush(buildAssetWithEntryDate("INV-002", LocalDate.now().minusDays(20))); // antes del corte

        Page<Asset> result = repository.findFiltered(
                null, null,
                LocalDate.now().minusDays(10),
                null,
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getInventoryNumber()).isEqualTo("INV-001");
    }

    @Test
    void should_combineStatusAndDateFilters_when_allParamsProvided() {
        // Cumple all: GOOD + ASSIGNED + dentro del rango
        entityManager.persistAndFlush(
                buildAssetWithAll("INV-001", ConditionStatus.GOOD, LifecycleStatus.ASSIGNED,
                        LocalDate.now().minusDays(5)));
        // Falla condición: BAD
        entityManager.persistAndFlush(
                buildAssetWithAll("INV-002", ConditionStatus.BAD, LifecycleStatus.ASSIGNED,
                        LocalDate.now().minusDays(5)));
        // Falla fecha: fuera de rango
        entityManager.persistAndFlush(
                buildAssetWithAll("INV-003", ConditionStatus.GOOD, LifecycleStatus.ASSIGNED,
                        LocalDate.now().minusDays(30)));

        Page<Asset> result = repository.findFiltered(
                ConditionStatus.GOOD, LifecycleStatus.ASSIGNED,
                LocalDate.now().minusDays(10),
                LocalDate.now(),
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getInventoryNumber()).isEqualTo("INV-001");
    }
    // =========================================================
    //  searchAssetsWithCurrentGuardian
    // =========================================================

    @Test
    void should_returnAllAssets_when_keywordIsEmpty() {
        entityManager.persistAndFlush(buildAsset("INV-001"));
        entityManager.persistAndFlush(buildAsset("INV-002"));

        Page<AssetSearchResponseDTO> result =
                repository.searchAssetsWithCurrentGuardian("", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void should_returnMatchingAsset_when_keywordMatchesInventoryNumber() {
        entityManager.persistAndFlush(buildAssetWithDescription("INV-2024-001", "Laptop Dell"));
        entityManager.persistAndFlush(buildAssetWithDescription("INV-2024-002", "Proyector Epson"));

        Page<AssetSearchResponseDTO> result =
                repository.searchAssetsWithCurrentGuardian("INV-2024-001", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).inventoryNumber()).isEqualTo("INV-2024-001");
    }

    @Test
    void should_returnMatchingAsset_when_keywordMatchesDescriptionCaseInsensitive() {
        entityManager.persistAndFlush(buildAssetWithDescription("INV-001", "Laptop Dell"));
        entityManager.persistAndFlush(buildAssetWithDescription("INV-002", "Proyector Epson"));

        Page<AssetSearchResponseDTO> result =
                repository.searchAssetsWithCurrentGuardian("LAPTOP", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).description()).isEqualTo("Laptop Dell");
    }

    @Test
    void should_populateCurrentGuardianName_when_assetHasActiveAssignment() {
        Asset asset       = entityManager.persistAndFlush(buildAsset("INV-001"));
        Guardian guardian = entityManager.persistAndFlush(buildGuardian("EMP-001", "Juan Pérez"));
        entityManager.persistAndFlush(buildActiveAssignment(asset, guardian));

        Page<AssetSearchResponseDTO> result =
                repository.searchAssetsWithCurrentGuardian("INV-001", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).currentGuardianName()).isEqualTo("Juan Pérez");
    }

    @Test
    void should_returnNullGuardianName_when_assetHasNoActiveAssignment() {
        Asset asset       = entityManager.persistAndFlush(buildAsset("INV-001"));
        Guardian guardian = entityManager.persistAndFlush(buildGuardian("EMP-001", "Juan Pérez"));
        // Asignación histórica (returnedAt != null) — no debe aparecer como guardián activo
        entityManager.persistAndFlush(buildReturnedAssignment(asset, guardian));

        Page<AssetSearchResponseDTO> result =
                repository.searchAssetsWithCurrentGuardian("INV-001", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).currentGuardianName()).isNull();
    }

    // =========================================================
    //  Count queries (dashboard)
    // =========================================================

    @Test
    void should_countCorrectly_when_queryingByLifecycleStatus() {
        entityManager.persistAndFlush(buildAssetWithStatus("INV-001", ConditionStatus.GOOD, LifecycleStatus.ASSIGNED));
        entityManager.persistAndFlush(buildAssetWithStatus("INV-002", ConditionStatus.GOOD, LifecycleStatus.ASSIGNED));
        entityManager.persistAndFlush(buildAssetWithStatus("INV-003", ConditionStatus.GOOD, LifecycleStatus.AVAILABLE));

        assertThat(repository.countByLifecycleStatus(LifecycleStatus.ASSIGNED)).isEqualTo(2);
    }

    @Test
    void should_excludeGivenStatus_when_countingByLifecycleStatusNot() {
        entityManager.persistAndFlush(buildAssetWithStatus("INV-001", ConditionStatus.GOOD, LifecycleStatus.ASSIGNED));
        entityManager.persistAndFlush(buildAssetWithStatus("INV-002", ConditionStatus.GOOD, LifecycleStatus.AVAILABLE));
        entityManager.persistAndFlush(buildAssetWithStatus("INV-003", ConditionStatus.GOOD, LifecycleStatus.DECOMMISSIONED));

        assertThat(repository.countByLifecycleStatusNot(LifecycleStatus.DECOMMISSIONED)).isEqualTo(2);
    }

    @Test
    void should_countOnlyMatchingConditionExcludingLifecycle_when_filteringForDashboard() {
        entityManager.persistAndFlush(buildAssetWithStatus("INV-001", ConditionStatus.BAD, LifecycleStatus.ASSIGNED));
        entityManager.persistAndFlush(buildAssetWithStatus("INV-002", ConditionStatus.BAD, LifecycleStatus.DECOMMISSIONED)); // excluido
        entityManager.persistAndFlush(buildAssetWithStatus("INV-003", ConditionStatus.GOOD, LifecycleStatus.ASSIGNED));      // excluido por condición

        long count = repository.countByConditionStatusAndLifecycleStatusNot(
                ConditionStatus.BAD, LifecycleStatus.DECOMMISSIONED);

        assertThat(count).isEqualTo(1);
    }

    // =========================================================
    //  findTopLocationsByAssignedAssets
    // =========================================================

    @Test
    void should_returnLocationsOrderedByAssetCountDesc_when_assignedAssetsExist() {
        Location sala1 = entityManager.persistAndFlush(buildLocation("Sala A"));
        Location sala2 = entityManager.persistAndFlush(buildLocation("Sala B"));

        // Sala A tiene 2 bienes asignados, Sala B tiene 1
        persistAssetAssignedAt(sala1, "INV-001");
        persistAssetAssignedAt(sala1, "INV-002");
        persistAssetAssignedAt(sala2, "INV-003");

        List<LocationStatDTO> result =
                repository.findTopLocationsByAssignedAssets(PageRequest.of(0, 10));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).locationName()).isEqualTo("Sala A");
        assertThat(result.get(0).assetCount()).isEqualTo(2);
        assertThat(result.get(1).assetCount()).isEqualTo(1);
    }

    @Test
    void should_respectPageLimit_when_manyLocationsExist() {
        persistAssetAssignedAt(entityManager.persistAndFlush(buildLocation("Sala A")), "INV-001");
        persistAssetAssignedAt(entityManager.persistAndFlush(buildLocation("Sala B")), "INV-002");
        persistAssetAssignedAt(entityManager.persistAndFlush(buildLocation("Sala C")), "INV-003");

        List<LocationStatDTO> result =
                repository.findTopLocationsByAssignedAssets(PageRequest.of(0, 2));

        assertThat(result).hasSize(2);
    }

    @Test
    void should_excludeNonAssignedAssets_when_queryingTopLocations() {
        Location location = entityManager.persistAndFlush(buildLocation("Sala A"));
        Asset available = buildAssetWithStatus("INV-001", ConditionStatus.GOOD, LifecycleStatus.AVAILABLE);
        available.setLocation(location);
        entityManager.persistAndFlush(available);

        List<LocationStatDTO> result =
                repository.findTopLocationsByAssignedAssets(PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }

    // =========================================================
    //  searchActive (typeahead)
    // =========================================================

    @Test
    void should_returnMatchingAssets_when_queryMatchesInventoryNumber() {
        entityManager.persistAndFlush(buildAssetWithDescription("INV-2024-001", "Laptop"));
        entityManager.persistAndFlush(buildAssetWithDescription("INV-2024-002", "Proyector"));

        List<AssetSearchResultDTO> result = repository.searchActive("INV-2024-001", 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).inventoryNumber()).isEqualTo("INV-2024-001");
    }

    @Test
    void should_excludeDecommissionedAssets_when_searchingActive() {
        entityManager.persistAndFlush(buildAssetWithStatus("INV-001", ConditionStatus.GOOD, LifecycleStatus.ASSIGNED));
        entityManager.persistAndFlush(buildAssetWithStatus("INV-002", ConditionStatus.GOOD, LifecycleStatus.DECOMMISSIONED));

        List<AssetSearchResultDTO> result = repository.searchActive("INV", 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).inventoryNumber()).isEqualTo("INV-001");
    }

    @Test
    void should_respectLimit_when_manyAssetsMatch() {
        for (int i = 1; i <= 5; i++) {
            entityManager.persistAndFlush(buildAsset("INV-00" + i));
        }

        List<AssetSearchResultDTO> result = repository.searchActive("INV", 3);

        assertThat(result).hasSize(3);
    }

    @Test
    void should_returnActiveAssetsExcludingDecommissioned_when_queryIsEmpty() {
        // Edge case documentado: query vacío devuelve primeros bienes no dados de baja
        entityManager.persistAndFlush(buildAssetWithStatus("INV-001", ConditionStatus.GOOD, LifecycleStatus.ASSIGNED));
        entityManager.persistAndFlush(buildAssetWithStatus("INV-002", ConditionStatus.GOOD, LifecycleStatus.DECOMMISSIONED));

        List<AssetSearchResultDTO> result = repository.searchActive("", 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).lifecycleStatus()).isNotEqualTo(LifecycleStatus.DECOMMISSIONED);
    }

    @Test
    void should_prioritizePrefixMatchFirst_when_inventoryNumberStartsWithQuery() {
        // "INV-001" empieza con "INV-0" → CASE WHEN = 0 (prioridad alta)
        // "OTRO-INV-002" contiene "INV-0" pero no empieza con él → CASE WHEN = 1
        entityManager.persistAndFlush(buildAssetWithDescription("OTRO-INV-002", "Bien secundario"));
        entityManager.persistAndFlush(buildAssetWithDescription("INV-001", "Bien primario"));

        List<AssetSearchResultDTO> result = repository.searchActive("INV-0", 10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).inventoryNumber()).isEqualTo("INV-001");
    }

    // =========================================================
    //  getNextSequence — EXCLUIDO INTENCIONALMENTE
    //  Razón: usa funciones nativas de MariaDB (REGEXP, SUBSTRING_INDEX,
    //  CAST(... AS UNSIGNED)) incompatibles con H2.
    //  Estrategia recomendada: integration test con Testcontainers + MariaDB
    //  cuando el pipeline permita levantar contenedores.
    // =========================================================

    // =========================================================
    //  Helpers
    // =========================================================











    private Asset buildAssetWithEntryDate(String inv, LocalDate entryDate) {
        Asset a = buildAsset(inv);
        a.setEntryDate(entryDate);
        return a;
    }

    private Asset buildAssetWithAll(String inv, ConditionStatus cond,
                                    LifecycleStatus life, LocalDate entryDate) {
        Asset a = buildAssetWithStatus(inv, cond, life);
        a.setEntryDate(entryDate);
        return a;
    }


    private Asset buildAssetWithDescription(String inv, String description) {
        Asset a = buildAsset(inv);
        a.setDescription(description);
        return a;
    }

    private AssetAssignment buildActiveAssignment(Asset asset, Guardian guardian) {
        AssetAssignment aa = new AssetAssignment();
        aa.setAsset(asset);
        aa.setGuardian(guardian);
        aa.setAssignedBy(operatorUser);
        aa.setAssignedAt(LocalDateTime.now().minusDays(5));
        aa.setReturnedAt(null);
        return aa;
    }

    private AssetAssignment buildReturnedAssignment(Asset asset, Guardian guardian) {
        AssetAssignment aa = buildActiveAssignment(asset, guardian);
        aa.setReturnedAt(LocalDateTime.now().minusDays(1));
        return aa;
    }

    /** Crea y persiste un bien con lifecycleStatus=ASSIGNED en la ubicación dada. */
    private void persistAssetAssignedAt(Location location, String inventoryNumber) {
        Asset a = buildAssetWithStatus(inventoryNumber, ConditionStatus.GOOD, LifecycleStatus.ASSIGNED);
        a.setLocation(location);
        entityManager.persistAndFlush(a);
    }
}
