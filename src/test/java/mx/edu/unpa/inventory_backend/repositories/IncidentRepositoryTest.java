package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.Incident;
import mx.edu.unpa.inventory_backend.domains.IncidentImage;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private IncidentRepository incidentRepository;

    private static final Pageable PAGE = PageRequest.of(0, 10);

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers locales
    // ─────────────────────────────────────────────────────────────────────────

    private Asset persistAsset(String inventoryNumber) {
        return entityManager.persistAndFlush(buildAsset(inventoryNumber));
    }

    /**
     * Construye una Incident mínima válida con estado OPEN.
     * No persiste — el caller puede ajustar campos antes de persistir.
     */
    private Incident buildIncident(Asset asset) {
        Incident i = new Incident();
        i.setAsset(asset);
        i.setIncidentDate(LocalDate.now());
        i.setDescription("Descripción de incidencia de prueba");
        i.setConditionAtIncident(ConditionStatus.BAD);
        i.setStatus(IncidentStatus.OPEN);
        i.setCreatedBy(operatorUser);
        return i;
    }

    /**
     * Construye una Incident con estado específico.
     * Útil para tests de filtrado donde el estado es el foco.
     */
    private Incident buildIncidentWithStatus(Asset asset, IncidentStatus status) {
        Incident i = buildIncident(asset);
        i.setStatus(status);
        return i;
    }

    /**
     * Construye y asocia una IncidentImage a una incidencia ya persistida.
     * No persiste — caller decide.
     */
    private IncidentImage buildImage(Incident incident, String fileName) {
        IncidentImage img = new IncidentImage();
        img.setIncident(incident);
        img.setFilePath("uploads/incidents/" + fileName);
        img.setFileName(fileName);
        img.setMimeType("image/jpeg");
        img.setUploadedBy(operatorUser);
        return img;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByIdWithDetails
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnIncident_when_idExistsWithNoImages() {
        // Arrange — incidencia sin imágenes, sin resolvedBy
        Asset asset = persistAsset("INV-001");
        Incident incident = entityManager.persistAndFlush(buildIncident(asset));
        entityManager.clear();

        // Act
        Optional<Incident> result = incidentRepository.findByIdWithDetails(incident.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAsset().getInventoryNumber()).isEqualTo("INV-001");
        assertThat(result.get().getCreatedBy().getUsername()).isEqualTo("operador01");
        assertThat(result.get().getResolvedBy()).isNull();
        assertThat(result.get().getImages()).isEmpty();
    }

    @Test
    void should_returnIncidentWithImagesLoaded_when_incidentHasImages() {
        // Verifica que LEFT JOIN FETCH i.images carga la colección completa.
        // Arrange
        Asset asset = persistAsset("INV-002");
        Incident incident = entityManager.persistAndFlush(buildIncident(asset));
        entityManager.persistAndFlush(buildImage(incident, "foto1.jpg"));
        entityManager.persistAndFlush(buildImage(incident, "foto2.jpg"));
        entityManager.clear();

        // Act
        Optional<Incident> result = incidentRepository.findByIdWithDetails(incident.getId());

        // Assert — la colección debe estar inicializada y accesible sin LazyInitializationException
        assertThat(result).isPresent();
        assertThat(result.get().getImages()).hasSize(2);
        assertThat(result.get().getImages())
                .extracting(IncidentImage::getFileName)
                .containsExactlyInAnyOrder("foto1.jpg", "foto2.jpg");
    }

    @Test
    void should_haveUploadedByLoadedOnImages_when_imagesExist() {
        // Verifica el segundo nivel de fetch: LEFT JOIN FETCH img.uploadedBy.
        // Sin este fetch, acceder a img.uploadedBy lanzaría LazyInitializationException.
        // Arrange
        Asset asset = persistAsset("INV-003");
        Incident incident = entityManager.persistAndFlush(buildIncident(asset));
        entityManager.persistAndFlush(buildImage(incident, "evidencia.jpg"));
        entityManager.clear();

        // Act
        Optional<Incident> result = incidentRepository.findByIdWithDetails(incident.getId());

        // Assert — acceder a uploadedBy no debe lanzar excepción
        assertThat(result).isPresent();
        assertThat(result.get().getImages().get(0).getUploadedBy()).isNotNull();
        assertThat(result.get().getImages().get(0).getUploadedBy().getUsername())
                .isEqualTo("operador01");
    }

    @Test
    void should_returnIncidentWithResolvedByLoaded_when_resolvedByIsPresent() {
        // Arrange
        Asset asset = persistAsset("INV-004");
        Incident incident = buildIncident(asset);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedBy(operatorUser);
        incident.setResolvedAt(LocalDateTime.now());
        entityManager.persistAndFlush(incident);
        entityManager.clear();

        // Act
        Optional<Incident> result = incidentRepository.findByIdWithDetails(incident.getId());

        // Assert — LEFT JOIN FETCH i.resolvedBy debe cargar el usuario
        assertThat(result).isPresent();
        assertThat(result.get().getResolvedBy()).isNotNull();
        assertThat(result.get().getResolvedBy().getUsername()).isEqualTo("operador01");
    }

    @Test
    void should_returnEmpty_when_idDoesNotExist() {
        // Act
        Optional<Incident> result = incidentRepository.findByIdWithDetails(Long.MAX_VALUE);

        // Assert
        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByAssetId (paginado)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnIncidentsForAsset_when_assetHasIncidents() {
        // Arrange
        Asset asset = persistAsset("INV-005");
        entityManager.persistAndFlush(buildIncident(asset));
        entityManager.persistAndFlush(buildIncident(asset));
        entityManager.clear();

        // Act
        Page<Incident> result = incidentRepository.findByAssetId(asset.getId(), PAGE);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .allSatisfy(i -> assertThat(i.getAsset().getId()).isEqualTo(asset.getId()));
    }

    @Test
    void should_returnEmpty_when_assetHasNoIncidents() {
        // Arrange
        Asset asset = persistAsset("INV-006");

        // Act
        Page<Incident> result = incidentRepository.findByAssetId(asset.getId(), PAGE);

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void should_returnOnlyIncidentsOfGivenAsset_when_multipleAssetsHaveIncidents() {
        // Verifica que el filtro por assetId no mezcla incidencias de otros bienes.
        // Arrange
        Asset asset1 = persistAsset("INV-007");
        Asset asset2 = persistAsset("INV-008");
        entityManager.persistAndFlush(buildIncident(asset1));
        entityManager.persistAndFlush(buildIncident(asset2)); // no debe aparecer
        entityManager.clear();

        // Act
        Page<Incident> result = incidentRepository.findByAssetId(asset1.getId(), PAGE);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAsset().getId()).isEqualTo(asset1.getId());
    }

    @Test
    void should_returnIncidentsOrderedByCreatedAtDesc_when_assetHasMultipleIncidents() {
        // createdAt inicializado inline — puede coincidir en tests rápidos.
        // JdbcTemplate fuerza timestamps distintos (Problema 2 del CONTEXTO).
        // Arrange
        Asset asset = persistAsset("INV-009");
        Incident older = entityManager.persistAndFlush(buildIncident(asset));
        Incident newer = entityManager.persistAndFlush(buildIncident(asset));

        jdbcTemplate.update(
                "UPDATE incidents SET created_at = ? WHERE id = ?",
                LocalDateTime.now().minusDays(1), older.getId()
        );
        jdbcTemplate.update(
                "UPDATE incidents SET created_at = ? WHERE id = ?",
                LocalDateTime.now(), newer.getId()
        );
        entityManager.clear();

        // Act
        Page<Incident> result = incidentRepository.findByAssetId(asset.getId(), PAGE);

        // Assert — ORDER BY createdAt DESC: la más reciente va primero
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(newer.getId());
        assertThat(result.getContent().get(1).getId()).isEqualTo(older.getId());
    }

    @Test
    void should_respectPagination_when_assetHasMultipleIncidents() {
        // Arrange — 3 incidencias; página de tamaño 2
        Asset asset = persistAsset("INV-010");
        entityManager.persistAndFlush(buildIncident(asset));
        entityManager.persistAndFlush(buildIncident(asset));
        entityManager.persistAndFlush(buildIncident(asset));
        entityManager.clear();

        Pageable firstPage = PageRequest.of(0, 2);

        // Act
        Page<Incident> result = incidentRepository.findByAssetId(asset.getId(), firstPage);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findAllFiltered (3 parámetros nullable simultáneos)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnAll_when_allFiltersAreNull() {
        // Edge case: los tres parámetros null deben comportarse como "sin filtro".
        //
        // NOTA: (:status IS NULL OR ...) con parámetro enum null puede tener problemas
        // de inferencia de tipo en Hibernate 6. Si este test falla con error de tipo
        // (no de assertion), el fix es reemplazar el @Query con un Specification<Incident>
        // o usar CAST(:status AS string) IS NULL en el JPQL.
        // Arrange
        Asset asset = persistAsset("INV-011");
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.OPEN));
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.RESOLVED));
        entityManager.clear();

        // Act
        Page<Incident> result = incidentRepository.findAllFiltered(null, null, null, PAGE);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void should_returnOnlyOpenIncidents_when_statusFilterIsOpen() {
        // Arrange
        Asset asset = persistAsset("INV-012");
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.OPEN));
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.CLOSED));
        entityManager.clear();

        // Act
        Page<Incident> result = incidentRepository
                .findAllFiltered(IncidentStatus.OPEN, null, null, PAGE);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(IncidentStatus.OPEN);
    }

    @Test
    void should_filterByAssetId_when_onlyAssetIdProvided() {
        // Arrange
        Asset asset1 = persistAsset("INV-013");
        Asset asset2 = persistAsset("INV-014");
        entityManager.persistAndFlush(buildIncident(asset1));
        entityManager.persistAndFlush(buildIncident(asset1));
        entityManager.persistAndFlush(buildIncident(asset2)); // no debe aparecer
        entityManager.clear();

        // Act
        Page<Incident> result = incidentRepository
                .findAllFiltered(null, asset1.getId(), null, PAGE);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .allSatisfy(i -> assertThat(i.getAsset().getId()).isEqualTo(asset1.getId()));
    }

    @Test
    void should_filterByIdFromFolio_when_onlyIdFromFolioProvided() {
        // El "folio" en el dominio es el ID de la incidencia formateado (ej: INC-00042).
        // El filtro recibe el ID numérico extraído del folio.
        // Arrange
        Asset asset = persistAsset("INV-015");
        Incident target = entityManager.persistAndFlush(buildIncident(asset));
        entityManager.persistAndFlush(buildIncident(asset)); // otra incidencia del mismo bien
        entityManager.clear();

        // Act
        Page<Incident> result = incidentRepository
                .findAllFiltered(null, null, target.getId(), PAGE);

        // Assert — solo debe retornar la incidencia con ese ID exacto
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(target.getId());
    }

    @Test
    void should_applyAllFiltersSimultaneously_when_allFiltersProvided() {
        // Verifica la combinación de los tres filtros activos al mismo tiempo.
        // Arrange
        Asset asset1 = persistAsset("INV-016");
        Asset asset2 = persistAsset("INV-017");

        Incident target = entityManager.persistAndFlush(
                buildIncidentWithStatus(asset1, IncidentStatus.IN_PROGRESS));

        // Señuelos que cada filtro individual debe excluir
        entityManager.persistAndFlush(
                buildIncidentWithStatus(asset1, IncidentStatus.RESOLVED));   // mismo asset, diferente status
        entityManager.persistAndFlush(
                buildIncidentWithStatus(asset2, IncidentStatus.IN_PROGRESS)); // diferente asset
        entityManager.clear();

        // Act — los tres filtros aplicados: status + assetId + folio del target
        Page<Incident> result = incidentRepository.findAllFiltered(
                IncidentStatus.IN_PROGRESS, asset1.getId(), target.getId(), PAGE);

        // Assert — solo el target cumple los tres criterios simultáneamente
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(target.getId());
    }

    @Test
    void should_returnEmpty_when_noIncidentsMatchFilters() {
        // Arrange
        Asset asset = persistAsset("INV-018");
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.OPEN));
        entityManager.clear();

        // Act — filtrar por CLOSED cuando solo existe OPEN
        Page<Incident> result = incidentRepository
                .findAllFiltered(IncidentStatus.CLOSED, null, null, PAGE);

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // countActiveByAssetId
    // ─────────────────────────────────────────────────────────────────────────

    // NOTA: este método usa literales de enum fully-qualified en JPQL:
    // mx.edu.unpa.inventory_backend.enums.IncidentStatus.OPEN
    // En Hibernate 6 + H2 esto es válido en JPQL estándar, pero si falla con
    // un error de parsing, el fix es reemplazar los literales por un parámetro:
    // WHERE i.status IN (:statuses) con @Param("statuses") List<IncidentStatus>

    @Test
    void should_countOpenAndInProgressIncidents_when_bothStatusesExist() {
        // Arrange
        Asset asset = persistAsset("INV-019");
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.OPEN));
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.IN_PROGRESS));
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.RESOLVED)); // no cuenta
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.CLOSED));   // no cuenta

        // Act
        long count = incidentRepository.countActiveByAssetId(asset.getId());

        // Assert — solo OPEN e IN_PROGRESS son "activas"
        assertThat(count).isEqualTo(2);
    }

    @Test
    void should_returnZero_when_assetHasNoActiveIncidents() {
        // Arrange — solo incidencias cerradas
        Asset asset = persistAsset("INV-020");
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.RESOLVED));
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.CLOSED));

        // Act
        long count = incidentRepository.countActiveByAssetId(asset.getId());

        // Assert
        assertThat(count).isZero();
    }

    @Test
    void should_countOnlyActiveIncidentsOfGivenAsset_when_multipleAssetsExist() {
        // Verifica que el conteo no suma activas de otros bienes.
        // Arrange
        Asset asset1 = persistAsset("INV-021");
        Asset asset2 = persistAsset("INV-022");

        entityManager.persistAndFlush(buildIncidentWithStatus(asset1, IncidentStatus.OPEN));
        entityManager.persistAndFlush(buildIncidentWithStatus(asset2, IncidentStatus.OPEN)); // no debe sumarse

        // Act
        long count = incidentRepository.countActiveByAssetId(asset1.getId());

        // Assert
        assertThat(count).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findAllByAssetId (lista sin paginar)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnAllIncidentsForAsset_when_assetHasMultipleIncidents() {
        // Arrange
        Asset asset = persistAsset("INV-023");
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.OPEN));
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.RESOLVED));
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.CLOSED));
        entityManager.clear();

        // Act
        List<Incident> result = incidentRepository.findAllByAssetId(asset.getId());

        // Assert — devuelve todas sin filtro de estado
        assertThat(result).hasSize(3);
    }


    @Test
    void should_returnIncidentsOrderedByCreatedAtDesc_when_assetHasMultiple() {
        // JdbcTemplate para garantizar timestamps distintos (Problema 2 del CONTEXTO).
        // Arrange
        Asset asset = persistAsset("INV-025");
        Incident older = entityManager.persistAndFlush(buildIncident(asset));
        Incident newer = entityManager.persistAndFlush(buildIncident(asset));

        jdbcTemplate.update(
                "UPDATE incidents SET created_at = ? WHERE id = ?",
                LocalDateTime.now().minusDays(1), older.getId()
        );
        jdbcTemplate.update(
                "UPDATE incidents SET created_at = ? WHERE id = ?",
                LocalDateTime.now(), newer.getId()
        );
        entityManager.clear();

        // Act
        List<Incident> result = incidentRepository.findAllByAssetId(asset.getId());

        // Assert — ORDER BY createdAt DESC: más reciente primero
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(newer.getId());
        assertThat(result.get(1).getId()).isEqualTo(older.getId());
    }

    @Test
    void should_returnOnlyIncidentsOfGivenAsset_when_multipleAssetsExist() {
        // Arrange
        Asset asset1 = persistAsset("INV-026");
        Asset asset2 = persistAsset("INV-027");
        entityManager.persistAndFlush(buildIncident(asset1));
        entityManager.persistAndFlush(buildIncident(asset2)); // no debe aparecer
        entityManager.clear();

        // Act
        List<Incident> result = incidentRepository.findAllByAssetId(asset1.getId());

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAsset().getId()).isEqualTo(asset1.getId());
    }

    @Test
    void should_haveCreatedByLoaded_when_incidentsAreReturned() {
        // Verifica que JOIN FETCH i.createdBy evita LazyInitializationException.
        // Arrange
        Asset asset = persistAsset("INV-028");
        entityManager.persistAndFlush(buildIncident(asset));
        entityManager.clear();

        // Act
        List<Incident> result = incidentRepository.findAllByAssetId(asset.getId());

        // Assert — acceder a createdBy no lanza excepción
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCreatedBy()).isNotNull();
        assertThat(result.get(0).getCreatedBy().getUsername()).isEqualTo("operador01");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // countByStatus (derivado)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnCorrectCount_when_incidentsWithStatusExist() {
        // Arrange
        Asset asset = persistAsset("INV-029");
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.OPEN));
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.OPEN));
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.RESOLVED));

        // Act
        long openCount = incidentRepository.countByStatus(IncidentStatus.OPEN);

        // Assert
        assertThat(openCount).isEqualTo(2);
    }

    @Test
    void should_returnZero_when_noIncidentsWithGivenStatusExist() {
        // Arrange — solo existen incidencias OPEN
        Asset asset = persistAsset("INV-030");
        entityManager.persistAndFlush(buildIncidentWithStatus(asset, IncidentStatus.OPEN));

        // Act
        long closedCount = incidentRepository.countByStatus(IncidentStatus.CLOSED);

        // Assert
        assertThat(closedCount).isZero();
    }
}