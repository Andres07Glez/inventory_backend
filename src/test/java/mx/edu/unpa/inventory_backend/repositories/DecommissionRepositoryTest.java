package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.AssetDecommission;
import mx.edu.unpa.inventory_backend.domains.Incident;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.DecommissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DecommissionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private DecommissionRepository decommissionRepository;

    private static final Pageable PAGE = PageRequest.of(0, 10);

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers locales
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Persiste y retorna un Asset listo para asociar a una baja.
     * Delega en buildAsset() de BaseRepositoryTest para respetar todos los
     * campos obligatorios (brand, category, createdBy, etc.).
     */
    private Asset persistAsset(String inventoryNumber) {
        return entityManager.persistAndFlush(buildAsset(inventoryNumber));
    }

    /**
     * Construye una baja PENDING sin incidencia de origen (caso más común).
     * No persiste — el caller decide el momento.
     */
    private AssetDecommission buildDecommission(Asset asset) {
        AssetDecommission d = new AssetDecommission();
        d.setAsset(asset);
        d.setJustification("Bien deteriorado más allá de reparación");
        d.setDecommissionDate(LocalDate.now());
        d.setStatus(DecommissionStatus.PENDING);
        d.setCreatedBy(operatorUser);
        return d;
    }

    /**
     * Construye una incidencia mínima válida para asociar a una baja.
     * conditionAtIncident es NOT NULL en la entidad — siempre usar BAD en tests de baja.
     */
    private Incident buildIncident(Asset asset) {
        Incident i = new Incident();
        i.setAsset(asset);
        i.setIncidentDate(LocalDate.now());
        i.setDescription("Daño severo por caída");
        i.setConditionAtIncident(ConditionStatus.BAD);
        i.setCreatedBy(operatorUser);
        return i;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByIdWithDetails
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnDecommission_when_idExistsWithoutOptionalRelations() {
        // Arrange — baja directa: sin incidencia de origen, sin confirmador
        Asset asset = persistAsset("INV-001");
        AssetDecommission decommission = entityManager.persistAndFlush(buildDecommission(asset));
        // clear() fuerza a Hibernate a leer desde BD en el Act,
        // evitando que devuelva el objeto ya cargado en el caché de primer nivel
        entityManager.clear();

        // Act
        Optional<AssetDecommission> result = decommissionRepository
                .findByIdWithDetails(decommission.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAsset().getInventoryNumber()).isEqualTo("INV-001");
        assertThat(result.get().getCreatedBy().getUsername()).isEqualTo("operador01");
        assertThat(result.get().getIncident()).isNull();
        assertThat(result.get().getConfirmedBy()).isNull();
    }

    @Test
    void should_returnDecommission_when_optionalIncidentIsPresent() {
        // Arrange — baja originada desde una incidencia previa
        Asset asset = persistAsset("INV-002");
        Incident incident = entityManager.persistAndFlush(buildIncident(asset));

        AssetDecommission decommission = buildDecommission(asset);
        decommission.setIncident(incident);
        entityManager.persistAndFlush(decommission);
        entityManager.clear();

        // Act
        Optional<AssetDecommission> result = decommissionRepository
                .findByIdWithDetails(decommission.getId());

        // Assert — LEFT JOIN FETCH d.incident debe cargar la incidencia
        assertThat(result).isPresent();
        assertThat(result.get().getIncident()).isNotNull();
        assertThat(result.get().getIncident().getId()).isEqualTo(incident.getId());
    }

    @Test
    void should_returnDecommission_when_confirmedByIsPresent() {
        // Arrange — baja en estado CONFIRMED con usuario confirmador
        Asset asset = persistAsset("INV-003");
        AssetDecommission decommission = buildDecommission(asset);
        decommission.setStatus(DecommissionStatus.CONFIRMED);
        decommission.setConfirmedBy(operatorUser);
        decommission.setConfirmedAt(LocalDateTime.now());
        entityManager.persistAndFlush(decommission);
        entityManager.clear();

        // Act
        Optional<AssetDecommission> result = decommissionRepository
                .findByIdWithDetails(decommission.getId());

        // Assert — LEFT JOIN FETCH d.confirmedBy debe cargar el confirmador
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(DecommissionStatus.CONFIRMED);
        assertThat(result.get().getConfirmedBy()).isNotNull();
        assertThat(result.get().getConfirmedBy().getUsername()).isEqualTo("operador01");
    }

    @Test
    void should_returnEmpty_when_idDoesNotExist() {
        // Act
        Optional<AssetDecommission> result = decommissionRepository
                .findByIdWithDetails(Long.MAX_VALUE);

        // Assert
        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // existsByAssetId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnTrue_when_decommissionExistsForAsset() {
        // Arrange
        Asset asset = persistAsset("INV-004");
        entityManager.persistAndFlush(buildDecommission(asset));

        // Act + Assert
        assertThat(decommissionRepository.existsByAssetId(asset.getId())).isTrue();
    }

    @Test
    void should_returnFalse_when_noDecommissionExistsForAsset() {
        // Arrange — asset sin ninguna baja asociada
        Asset asset = persistAsset("INV-005");

        // Act + Assert
        assertThat(decommissionRepository.existsByAssetId(asset.getId())).isFalse();
    }

    @Test
    void should_returnTrue_regardless_of_decommissionStatus() {
        // Edge case: el contrato del método es "¿existe alguna baja para este bien?"
        // sin importar el estado. Un bien con baja CONFIRMED también debe retornar true.
        // Arrange
        Asset asset = persistAsset("INV-006");
        AssetDecommission confirmed = buildDecommission(asset);
        confirmed.setStatus(DecommissionStatus.CONFIRMED);
        entityManager.persistAndFlush(confirmed);

        // Act + Assert
        assertThat(decommissionRepository.existsByAssetId(asset.getId())).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByAssetId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnDecommission_when_decommissionExistsForAsset() {
        // Arrange
        Asset asset = persistAsset("INV-007");
        AssetDecommission decommission = entityManager.persistAndFlush(buildDecommission(asset));
        entityManager.clear();

        // Act
        Optional<AssetDecommission> result = decommissionRepository
                .findByAssetId(asset.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(decommission.getId());
        assertThat(result.get().getCreatedBy()).isNotNull();
    }

    @Test
    void should_returnDecommission_withIncidentLoaded_when_incidentLinked() {
        // Arrange — verifica que LEFT JOIN FETCH d.incident carga la relación opcional
        Asset asset = persistAsset("INV-008");
        Incident incident = entityManager.persistAndFlush(buildIncident(asset));
        AssetDecommission decommission = buildDecommission(asset);
        decommission.setIncident(incident);
        entityManager.persistAndFlush(decommission);
        entityManager.clear();

        // Act
        Optional<AssetDecommission> result = decommissionRepository
                .findByAssetId(asset.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getIncident()).isNotNull();
        assertThat(result.get().getIncident().getId()).isEqualTo(incident.getId());
    }

    @Test
    void should_returnEmpty_when_assetHasNoDecommission() {
        // Arrange — asset existe pero sin baja
        Asset asset = persistAsset("INV-009");

        // Act
        Optional<AssetDecommission> result = decommissionRepository
                .findByAssetId(asset.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findAllFiltered (paginación + filtro opcional por estado)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnOnlyPending_when_filterIsPending() {
        // Arrange
        Asset asset1 = persistAsset("INV-010");
        Asset asset2 = persistAsset("INV-011");

        entityManager.persistAndFlush(buildDecommission(asset1)); // PENDING

        AssetDecommission confirmed = buildDecommission(asset2);
        confirmed.setStatus(DecommissionStatus.CONFIRMED);
        entityManager.persistAndFlush(confirmed);
        entityManager.clear();

        // Act
        Page<AssetDecommission> result = decommissionRepository
                .findAllFiltered(DecommissionStatus.PENDING, PAGE);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(DecommissionStatus.PENDING);
    }

    @Test
    void should_returnOnlyConfirmed_when_filterIsConfirmed() {
        // Arrange
        Asset asset1 = persistAsset("INV-012");
        Asset asset2 = persistAsset("INV-013");

        entityManager.persistAndFlush(buildDecommission(asset1)); // PENDING

        AssetDecommission confirmed = buildDecommission(asset2);
        confirmed.setStatus(DecommissionStatus.CONFIRMED);
        entityManager.persistAndFlush(confirmed);
        entityManager.clear();

        // Act
        Page<AssetDecommission> result = decommissionRepository
                .findAllFiltered(DecommissionStatus.CONFIRMED, PAGE);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(DecommissionStatus.CONFIRMED);
    }

    @Test
    void should_returnAll_when_filterIsNull() {
        // Edge case crítico: null como parámetro de enum debe comportarse como "sin filtro".
        //
        // NOTA DE IMPLEMENTACIÓN: (:status IS NULL OR d.status = :status) en JPQL puede
        // tener problemas de inferencia de tipo en Hibernate 6 cuando el valor es null,
        // ya que Hibernate no puede determinar el tipo del parámetro.
        // Si este test falla con un error de tipo (no de assertion), el fix es:
        //   @Query("... WHERE (CAST(:status AS string) IS NULL OR d.status = :status) ...")
        // o usar un Specification en lugar del @Query.
        // Arrange
        Asset asset1 = persistAsset("INV-014");
        Asset asset2 = persistAsset("INV-015");

        entityManager.persistAndFlush(buildDecommission(asset1));

        AssetDecommission confirmed = buildDecommission(asset2);
        confirmed.setStatus(DecommissionStatus.CONFIRMED);
        entityManager.persistAndFlush(confirmed);
        entityManager.clear();

        // Act
        Page<AssetDecommission> result = decommissionRepository
                .findAllFiltered(null, PAGE);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void should_returnEmpty_when_noDecommissionsMatchFilter() {
        // Arrange — solo existe una baja PENDING
        Asset asset = persistAsset("INV-016");
        entityManager.persistAndFlush(buildDecommission(asset));
        entityManager.clear();

        // Act — se filtra por CONFIRMED, que no tiene registros
        Page<AssetDecommission> result = decommissionRepository
                .findAllFiltered(DecommissionStatus.CONFIRMED, PAGE);

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void should_returnDecommissionsOrderedByCreatedAtDesc() {
        // createdAt se inicializa inline (= LocalDateTime.now()) — no via @PrePersist.
        // En tests rápidos, dos entidades creadas consecutivamente pueden tener el mismo
        // timestamp, haciendo el orden no determinístico.
        // Solución (Problema 2 del CONTEXTO): JdbcTemplate fuerza valores distintos
        // directamente en BD, luego entityManager.clear() invalida el caché.

        // Arrange
        Asset asset1 = persistAsset("INV-017");
        Asset asset2 = persistAsset("INV-018");

        AssetDecommission older = entityManager.persistAndFlush(buildDecommission(asset1));
        AssetDecommission newer = entityManager.persistAndFlush(buildDecommission(asset2));

        jdbcTemplate.update(
                "UPDATE asset_decommissions SET created_at = ? WHERE id = ?",
                LocalDateTime.now().minusDays(1), older.getId()
        );
        jdbcTemplate.update(
                "UPDATE asset_decommissions SET created_at = ? WHERE id = ?",
                LocalDateTime.now(), newer.getId()
        );
        entityManager.clear();

        // Act
        Page<AssetDecommission> result = decommissionRepository
                .findAllFiltered(null, PAGE);

        // Assert — ORDER BY d.createdAt DESC: el más reciente va primero
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(newer.getId());
        assertThat(result.getContent().get(1).getId()).isEqualTo(older.getId());
    }

    @Test
    void should_respectPagination_when_multipleDecommissionsExist() {
        // Arrange — 3 bajas; página de tamaño 2
        Asset asset1 = persistAsset("INV-019");
        Asset asset2 = persistAsset("INV-020");
        Asset asset3 = persistAsset("INV-021");

        entityManager.persistAndFlush(buildDecommission(asset1));
        entityManager.persistAndFlush(buildDecommission(asset2));
        entityManager.persistAndFlush(buildDecommission(asset3));
        entityManager.clear();

        Pageable firstPage = PageRequest.of(0, 2);

        // Act
        Page<AssetDecommission> result = decommissionRepository
                .findAllFiltered(null, firstPage);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }
}