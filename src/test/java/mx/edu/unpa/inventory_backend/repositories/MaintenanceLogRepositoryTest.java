package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.Incident;
import mx.edu.unpa.inventory_backend.domains.MaintenanceLog;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import mx.edu.unpa.inventory_backend.enums.MaintenanceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


class MaintenanceLogRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private MaintenanceLogRepository maintenanceLogRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    // ── Fixtures ────────────────────────────────────────────────────────────────

    private Asset asset;
    private Asset otherAsset;

    @BeforeEach
    void setUpFixtures() {
        asset      = entityManager.persistAndFlush(buildAsset("INV-TEST-001"));
        otherAsset = entityManager.persistAndFlush(buildAsset("INV-TEST-002"));
        entityManager.clear();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Construye y persiste un MaintenanceLog mínimo válido para el asset dado.
     * No persiste — el caller decide cuándo hacer flush.
     */
    private MaintenanceLog buildLog(Asset target, MaintenanceType type, LocalDate performedDate) {
        var log = new MaintenanceLog();
        log.setAsset(target);
        log.setMaintenanceType(type);
        log.setDescription("Mantenimiento de prueba - " + type);
        log.setPerformedDate(performedDate);
        log.setCreatedBy(operatorUser);
        return log;
    }

    /**
     * Persiste un Incident mínimo válido para el asset dado.
     */
    private Incident persistIncident(Asset target) {
        var incident = new Incident();
        incident.setAsset(target);
        incident.setIncidentDate(LocalDate.now());
        incident.setDescription("Incidencia de prueba");
        incident.setStatus(IncidentStatus.OPEN);
        incident.setConditionAtIncident(ConditionStatus.REGULAR);
        incident.setCreatedBy(operatorUser);
        return entityManager.persistAndFlush(incident);
    }

    /**
     * Fuerza el valor de created_at vía SQL nativo para controlar el ordenamiento.
     * Necesario porque @CreationTimestamp es controlado por Hibernate (updatable=false)
     * y no responde a setters post-persist.
     */
    private void forceCreatedAt(Long logId, LocalDateTime value) {
        jdbcTemplate.update(
                "UPDATE maintenance_logs SET created_at = ? WHERE id = ?",
                value, logId
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // findByAssetIdOrdered
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_returnLogsForAsset_when_multipleLogsExistForDifferentAssets() {
        // Arrange
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.PREVENTIVE, LocalDate.now()));
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.CORRECTIVE, LocalDate.now().minusDays(1)));
        entityManager.persistAndFlush(buildLog(otherAsset, MaintenanceType.WARRANTY, LocalDate.now()));
        entityManager.clear();

        // Act
        List<MaintenanceLog> result = maintenanceLogRepository.findByAssetIdOrdered(asset.getId());

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(m -> m.getAsset().getId())
                .containsOnly(asset.getId());
    }

    @Test
    void should_returnEmpty_when_assetHasNoMaintenanceLogs() {
        // Arrange — otherAsset tiene un log, asset no tiene ninguno
        entityManager.persistAndFlush(buildLog(otherAsset, MaintenanceType.PREVENTIVE, LocalDate.now()));
        entityManager.clear();

        // Act
        List<MaintenanceLog> result = maintenanceLogRepository.findByAssetIdOrdered(asset.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_orderByPerformedDateDesc_when_logsHaveDifferentPerformedDates() {
        // Arrange
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.PREVENTIVE, LocalDate.now().minusDays(10)));
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.CORRECTIVE, LocalDate.now().minusDays(5)));
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.WARRANTY,   LocalDate.now()));
        entityManager.clear();

        // Act
        List<MaintenanceLog> result = maintenanceLogRepository.findByAssetIdOrdered(asset.getId());

        // Assert — orden: newest → middle → older
        assertThat(result).extracting(MaintenanceLog::getPerformedDate)
                .containsExactly(
                        LocalDate.now(),
                        LocalDate.now().minusDays(5),
                        LocalDate.now().minusDays(10)
                );
    }

    @Test
    void should_orderByCreatedAtDesc_when_logsShareSamePerformedDate() {
        // Arrange — misma performedDate, distintos createdAt
        var first  = entityManager.persistAndFlush(buildLog(asset, MaintenanceType.PREVENTIVE, LocalDate.now()));
        var second = entityManager.persistAndFlush(buildLog(asset, MaintenanceType.CORRECTIVE, LocalDate.now()));

        // Forzamos created_at para garantizar orden determinista
        forceCreatedAt(first.getId(),  LocalDateTime.now().minusHours(2));
        forceCreatedAt(second.getId(), LocalDateTime.now().minusHours(1));
        entityManager.clear();

        // Act
        List<MaintenanceLog> result = maintenanceLogRepository.findByAssetIdOrdered(asset.getId());

        // Assert — second fue creado más recientemente → debe ir primero
        assertThat(result.get(0).getId()).isEqualTo(second.getId());
        assertThat(result.get(1).getId()).isEqualTo(first.getId());
    }

    @Test
    void should_fetchAssetAndCreatedByEagerly_when_findByAssetIdOrdered() {
        // Arrange
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.PREVENTIVE, LocalDate.now()));
        entityManager.clear();

        // Act
        List<MaintenanceLog> result = maintenanceLogRepository.findByAssetIdOrdered(asset.getId());

        // Assert — no LazyInitializationException al acceder a las asociaciones feched
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAsset().getId()).isEqualTo(asset.getId());
        assertThat(result.get(0).getCreatedBy().getId()).isEqualTo(operatorUser.getId());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // findAllFiltered
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_returnOnlyMatchingType_when_typeFilterIsProvided() {
        // Arrange
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.PREVENTIVE, LocalDate.now()));
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.CORRECTIVE, LocalDate.now()));
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.WARRANTY,   LocalDate.now()));
        entityManager.clear();

        // Act
        List<MaintenanceLog> result = maintenanceLogRepository.findAllFiltered(MaintenanceType.PREVENTIVE);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMaintenanceType()).isEqualTo(MaintenanceType.PREVENTIVE);
    }

    @Test
    void should_returnAllLogs_when_typeFilterIsNull() {
        // Arrange
        entityManager.persistAndFlush(buildLog(asset,      MaintenanceType.PREVENTIVE, LocalDate.now()));
        entityManager.persistAndFlush(buildLog(asset,      MaintenanceType.CORRECTIVE, LocalDate.now()));
        entityManager.persistAndFlush(buildLog(otherAsset, MaintenanceType.WARRANTY,   LocalDate.now()));
        entityManager.clear();

        // Act
        // NOTA: el patrón (:type IS NULL OR ...) en Hibernate 6 puede ser inestable
        // cuando type=null para parámetros enum. Si este test falla en CI con un
        // HibernateQueryException, la solución recomendada es separar en dos métodos:
        // findAll(Pageable) y findByMaintenanceType(type, Pageable).
        List<MaintenanceLog> result = maintenanceLogRepository.findAllFiltered(null);

        // Assert
        assertThat(result).hasSize(3);
    }

    @Test
    void should_returnEmpty_when_noLogsMatchType() {
        // Arrange — solo PREVENTIVE, buscamos WARRANTY
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.PREVENTIVE, LocalDate.now()));
        entityManager.clear();

        // Act
        List<MaintenanceLog> result = maintenanceLogRepository.findAllFiltered(MaintenanceType.WARRANTY);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_orderByPerformedDateDesc_when_findAllFiltered() {
        // Arrange
        var older  = entityManager.persistAndFlush(buildLog(asset, MaintenanceType.CORRECTIVE, LocalDate.now().minusDays(5)));
        var newer  = entityManager.persistAndFlush(buildLog(asset, MaintenanceType.CORRECTIVE, LocalDate.now()));
        entityManager.clear();

        // Act
        List<MaintenanceLog> result = maintenanceLogRepository.findAllFiltered(MaintenanceType.CORRECTIVE);

        // Assert
        assertThat(result.get(0).getId()).isEqualTo(newer.getId());
        assertThat(result.get(1).getId()).isEqualTo(older.getId());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // findByIdWithDetails
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_returnLogWithAllDetails_when_idExists() {
        // Arrange
        var incident = persistIncident(asset);
        var log = buildLog(asset, MaintenanceType.CORRECTIVE, LocalDate.now());
        log.setIncident(incident);
        log.setCost(BigDecimal.valueOf(1500.00));
        log.setConditionBefore(ConditionStatus.BAD);
        log.setConditionAfter(ConditionStatus.GOOD);
        var persisted = entityManager.persistAndFlush(log);
        entityManager.clear();

        // Act
        Optional<MaintenanceLog> result = maintenanceLogRepository.findByIdWithDetails(persisted.getId());

        // Assert
        assertThat(result).isPresent();
        var found = result.get();
        assertThat(found.getAsset().getId()).isEqualTo(asset.getId());
        assertThat(found.getCreatedBy().getId()).isEqualTo(operatorUser.getId());
        assertThat(found.getIncident()).isNotNull();
        assertThat(found.getIncident().getId()).isEqualTo(incident.getId());
        assertThat(found.getCost()).isEqualByComparingTo(BigDecimal.valueOf(1500.00));
        assertThat(found.getConditionBefore()).isEqualTo(ConditionStatus.BAD);
        assertThat(found.getConditionAfter()).isEqualTo(ConditionStatus.GOOD);
    }

    @Test
    void should_returnLogWithNullIncident_when_logHasNoLinkedIncident() {
        // Arrange — log sin incident (campo nullable)
        var log = buildLog(asset, MaintenanceType.PREVENTIVE, LocalDate.now());
        // incident queda null intencionalmente
        var persisted = entityManager.persistAndFlush(log);
        entityManager.clear();

        // Act
        Optional<MaintenanceLog> result = maintenanceLogRepository.findByIdWithDetails(persisted.getId());

        // Assert — LEFT JOIN FETCH: no debe lanzar excepción, incident es null
        assertThat(result).isPresent();
        assertThat(result.get().getIncident()).isNull();
    }

    @Test
    void should_returnEmpty_when_idDoesNotExist() {
        // Act
        Optional<MaintenanceLog> result = maintenanceLogRepository.findByIdWithDetails(999L);

        // Assert
        assertThat(result).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // existsIncidentLinkedToOtherAsset
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_returnFalse_when_incidentBelongsToSameAsset() {
        // Arrange — incident del mismo asset, log vinculado a ese mismo asset
        var incident = persistIncident(asset);
        var log = buildLog(asset, MaintenanceType.CORRECTIVE, LocalDate.now());
        log.setIncident(incident);
        entityManager.persistAndFlush(log);
        entityManager.clear();

        // Act — preguntamos si incident está vinculado a un asset DISTINTO de asset.getId()
        boolean result = maintenanceLogRepository.existsIncidentLinkedToOtherAsset(
                incident.getId(), asset.getId()
        );

        // Assert — false: el único log que tiene este incident es del mismo asset
        assertThat(result).isFalse();
    }

    @Test
    void should_returnTrue_when_incidentIsLinkedToADifferentAsset() {
        // Arrange — incident del asset original, pero el log lo vinculamos a otherAsset
        // (simula un dato corrupto o una validación que falló antes)
        var incident = persistIncident(asset);
        var log = buildLog(otherAsset, MaintenanceType.CORRECTIVE, LocalDate.now());
        log.setIncident(incident);
        entityManager.persistAndFlush(log);
        entityManager.clear();

        // Act — preguntamos si incident está vinculado a un asset distinto de asset.getId()
        boolean result = maintenanceLogRepository.existsIncidentLinkedToOtherAsset(
                incident.getId(), asset.getId()
        );

        // Assert — true: hay un log con ese incident cuyo asset es otherAsset (≠ asset)
        assertThat(result).isTrue();
    }

    @Test
    void should_returnFalse_when_incidentHasNoLinkedMaintenanceLogs() {
        // Arrange — incident sin ningún log asociado
        var incident = persistIncident(asset);
        entityManager.clear();

        // Act
        boolean result = maintenanceLogRepository.existsIncidentLinkedToOtherAsset(
                incident.getId(), asset.getId()
        );

        // Assert
        assertThat(result).isFalse();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // countByPerformedDateBetween
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_countCorrectly_when_logsAreWithinDateRange() {
        // Arrange
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.PREVENTIVE, LocalDate.now().minusDays(5)));
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.CORRECTIVE, LocalDate.now().minusDays(3)));
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.WARRANTY,   LocalDate.now().minusDays(1)));
        // Fuera del rango
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.PREVENTIVE, LocalDate.now().minusDays(10)));
        entityManager.clear();

        // Act
        long count = maintenanceLogRepository.countByPerformedDateBetween(
                LocalDate.now().minusDays(5),
                LocalDate.now().minusDays(1)
        );

        // Assert — los límites son inclusivos (Spring Data BETWEEN)
        assertThat(count).isEqualTo(3);
    }

    @Test
    void should_returnZero_when_noLogsWithinDateRange() {
        // Arrange — todos fuera del rango
        entityManager.persistAndFlush(buildLog(asset, MaintenanceType.PREVENTIVE, LocalDate.now().minusDays(30)));
        entityManager.clear();

        // Act
        long count = maintenanceLogRepository.countByPerformedDateBetween(
                LocalDate.now().minusDays(7),
                LocalDate.now()
        );

        // Assert
        assertThat(count).isZero();
    }
}