package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


class AssetAssignmentRepositoryTest extends BaseRepositoryTest{

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AssetAssignmentRepository repository;

    // --- Fixtures compartidos ---
    private Asset asset;
    private Asset otherAsset;
    private Guardian guardian;
    private Guardian otherGuardian;

    @BeforeEach
    void setUp() {

        guardian      = entityManager.persistAndFlush(buildGuardian("EMP-001", "Juan Pérez"));
        otherGuardian = entityManager.persistAndFlush(buildGuardian("EMP-002", "María López"));
        asset         = entityManager.persistAndFlush(buildAsset("INV-2024-001"));
        otherAsset    = entityManager.persistAndFlush(buildAsset("INV-2024-002"));
    }

    // =========================================================
    //  findActiveByAssetId
    // =========================================================

    @Test
    void should_returnActiveAssignment_when_assignmentHasNullReturnedAt() {
        // Arrange
        entityManager.persistAndFlush(buildAssignment(asset, guardian, null));

        // Act
        Optional<AssetAssignment> result = repository.findActiveByAssetId(asset.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getReturnedAt()).isNull();
        assertThat(result.get().getGuardian().getId()).isEqualTo(guardian.getId());
    }

    @Test
    void should_returnEmpty_when_allAssignmentsForAssetAreReturned() {
        // Arrange
        entityManager.persistAndFlush(
                buildAssignment(asset, guardian, LocalDateTime.now().minusDays(5))
        );

        // Act
        Optional<AssetAssignment> result = repository.findActiveByAssetId(asset.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_assetHasNoAssignmentsAtAll() {
        // Act
        Optional<AssetAssignment> result = repository.findActiveByAssetId(asset.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnOnlyActive_when_assetHasBothReturnedAndActiveAssignments() {
        // Arrange — simula rotación real: devuelto y luego reasignado
        entityManager.persistAndFlush(
                buildAssignment(asset, otherGuardian, LocalDateTime.now().minusDays(10))
        );
        entityManager.persistAndFlush(buildAssignment(asset, guardian, null));

        // Act
        Optional<AssetAssignment> result = repository.findActiveByAssetId(asset.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getGuardian().getId()).isEqualTo(guardian.getId());
    }

    @Test
    void should_notReturnActiveAssignment_when_itBelongsToADifferentAsset() {
        // Arrange — el bien activo es de otherAsset, no del asset bajo prueba
        entityManager.persistAndFlush(buildAssignment(otherAsset, guardian, null));

        // Act
        Optional<AssetAssignment> result = repository.findActiveByAssetId(asset.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_eagerlyResolveGuardian_when_sessionIsClearedAfterPersist() {
        // Arrange
        entityManager.persistAndFlush(buildAssignment(asset, guardian, null));
        // Forzar recarga real desde BD (descarta el caché de 1er nivel de Hibernate)
        entityManager.clear();

        // Act
        Optional<AssetAssignment> result = repository.findActiveByAssetId(asset.getId());

        // Assert — si JOIN FETCH se eliminara del query, esto lanzaría LazyInitializationException
        assertThat(result).isPresent();
        assertThat(result.get().getGuardian().getFullName()).isNotBlank();
    }

    // =========================================================
    //  findAllByAssetIdOrderByActivity
    // =========================================================

    @Test
    void should_returnEmptyList_when_assetHasNoAssignments() {
        // Act
        List<AssetAssignment> result = repository.findAllByAssetIdOrderByActivity(asset.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnAllAssignments_when_assetHasMultipleAssignments() {
        // Arrange
        entityManager.persistAndFlush(
                buildAssignment(asset, otherGuardian, LocalDateTime.now().minusDays(15))
        );
        entityManager.persistAndFlush(buildAssignment(asset, guardian, null));

        // Act
        List<AssetAssignment> result = repository.findAllByAssetIdOrderByActivity(asset.getId());

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void should_placeActiveAssignmentFirst_when_mixedReturnedAtValues() {
        // Arrange — ORDER BY returnedAt NULLS FIRST debe colocar el activo al inicio
        entityManager.persistAndFlush(
                buildAssignmentWithDates(
                        asset, otherGuardian,
                        LocalDateTime.now().minusDays(20),
                        LocalDateTime.now().minusDays(10)  // devuelto
                )
        );
        entityManager.persistAndFlush(
                buildAssignmentWithDates(
                        asset, guardian,
                        LocalDateTime.now().minusDays(5),
                        null                               // activo
                )
        );

        // Act
        List<AssetAssignment> result = repository.findAllByAssetIdOrderByActivity(asset.getId());

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getReturnedAt()).isNull();      // activo primero
        assertThat(result.get(1).getReturnedAt()).isNotNull();   // devuelto al final
    }

    @Test
    void should_orderReturnedAssignmentsByAssignedAtDesc_when_multipleReturnedAssignmentsExist() {
        // Arrange — mismo returnedAt para que el sort secundario (assignedAt DESC) sea el que decida
        LocalDateTime sharedReturnedAt = LocalDateTime.now().minusDays(1);
        LocalDateTime olderAssignedAt  = LocalDateTime.now().minusDays(30);
        LocalDateTime newerAssignedAt  = LocalDateTime.now().minusDays(10);

        entityManager.persistAndFlush(
                buildAssignmentWithDates(asset, otherGuardian, olderAssignedAt, sharedReturnedAt)
        );
        entityManager.persistAndFlush(
                buildAssignmentWithDates(asset, guardian, newerAssignedAt, sharedReturnedAt)
        );

        // Act
        List<AssetAssignment> result = repository.findAllByAssetIdOrderByActivity(asset.getId());

        // Assert — con mismo returnedAt, assignedAt DESC debe colocar la más reciente primero
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAssignedAt())
                .isAfterOrEqualTo(result.get(1).getAssignedAt());
    }

    @Test
    void should_excludeAssignmentsFromOtherAssets_when_filteringByAssetId() {
        // Arrange
        entityManager.persistAndFlush(buildAssignment(otherAsset, guardian, null));
        entityManager.persistAndFlush(buildAssignment(asset, otherGuardian, null));

        // Act
        List<AssetAssignment> result = repository.findAllByAssetIdOrderByActivity(asset.getId());

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAsset().getId()).isEqualTo(asset.getId());
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private AssetAssignment buildAssignment(Asset asset, Guardian guardian, LocalDateTime returnedAt) {
        return buildAssignmentWithDates(asset, guardian, LocalDateTime.now().minusDays(30), returnedAt);
    }

    private AssetAssignment buildAssignmentWithDates(Asset asset, Guardian guardian,
                                                     LocalDateTime assignedAt,
                                                     LocalDateTime returnedAt) {
        AssetAssignment aa = new AssetAssignment();
        aa.setAsset(asset);
        aa.setGuardian(guardian);
        aa.setAssignedBy(operatorUser);  // nullable=false en BD, requerido
        aa.setAssignedAt(assignedAt);
        aa.setReturnedAt(returnedAt);
        return aa;
    }
}