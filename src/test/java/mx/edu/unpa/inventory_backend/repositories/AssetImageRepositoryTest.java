package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.AssetImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AssetImageRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AssetImageRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Asset asset;
    private Asset otherAsset;

    @BeforeEach
    void setUp() {
        asset        = entityManager.persistAndFlush(buildAsset("INV-IMG-001"));
        otherAsset   = entityManager.persistAndFlush(buildAsset("INV-IMG-002"));
    }

    // =========================================================
    //  findByAssetIdOrderByIsPrimaryDescUploadedAtAsc
    // =========================================================

    @Test
    void should_returnEmptyList_when_assetHasNoImages() {
        List<AssetImage> result =
                repository.findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(asset.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnPrimaryImageFirst_when_mixedPrimaryAndNonPrimaryImages() {
        // Arrange — isPrimary DESC: la imagen primaria debe aparecer primero
        AssetImage nonPrimary = persistImageWithUploadedAt(asset, false,
                LocalDateTime.now().minusHours(2));
        AssetImage primary    = persistImageWithUploadedAt(asset, true,
                LocalDateTime.now().minusHours(1)); // más reciente, pero es primary

        entityManager.clear();
        // Act
        List<AssetImage> result =
                repository.findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(asset.getId());

        // Assert — primary va primero sin importar uploadedAt
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(primary.getId());
        assertThat(result.get(1).getId()).isEqualTo(nonPrimary.getId());
    }

    // ---- Test corregido ----

    @Test
    void should_returnNonPrimaryImagesOrderedByUploadedAtAsc_when_noPrimaryImage() {
        // Arrange
        LocalDateTime earlier = LocalDateTime.now().minusHours(3);
        LocalDateTime later   = LocalDateTime.now().minusHours(1);

        AssetImage laterImage   = persistImageWithUploadedAt(asset, false, later);
        AssetImage earlierImage = persistImageWithUploadedAt(asset, false, earlier);

        // Vaciar caché de 1er nivel para que el SELECT lea los valores reales de BD,
        // no las instancias con uploadedAt = now() que Hibernate tiene en memoria.
        entityManager.clear();

        // Act
        List<AssetImage> result =
                repository.findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(asset.getId());

        // Assert — la más antigua debe aparecer primero (ASC)
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(earlierImage.getId());
        assertThat(result.get(1).getId()).isEqualTo(laterImage.getId());
    }

    @Test
    void should_notIncludeImagesFromOtherAssets_when_queryingByAssetId() {
        // Arrange
        persistImageWithUploadedAt(otherAsset, true, LocalDateTime.now());
        persistImageWithUploadedAt(asset, false, LocalDateTime.now());

        // Act
        List<AssetImage> result =
                repository.findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(asset.getId());

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAsset().getId()).isEqualTo(asset.getId());
    }

    // =========================================================
    //  countByAssetId
    // =========================================================

    @Test
    void should_returnCorrectCount_when_assetHasMultipleImages() {
        // Arrange
        persistImageWithUploadedAt(asset, true,  LocalDateTime.now().minusHours(2));
        persistImageWithUploadedAt(asset, false, LocalDateTime.now().minusHours(1));
        persistImageWithUploadedAt(otherAsset, false, LocalDateTime.now()); // no debe contarse

        // Act
        long count = repository.countByAssetId(asset.getId());

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    void should_returnZero_when_assetHasNoImages() {
        assertThat(repository.countByAssetId(asset.getId())).isZero();
    }

    // =========================================================
    //  findByIdAndAssetId
    // =========================================================

    @Test
    void should_returnImage_when_imageExistsAndBelongsToAsset() {
        // Arrange
        AssetImage image = persistImageWithUploadedAt(asset, false, LocalDateTime.now());

        // Act
        Optional<AssetImage> result = repository.findByIdAndAssetId(image.getId(), asset.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(image.getId());
    }

    @Test
    void should_returnEmpty_when_imageDoesNotBelongToAsset() {
        // Arrange — imagen registrada bajo otherAsset, se consulta con asset.getId()
        AssetImage imageOfOther = persistImageWithUploadedAt(otherAsset, false, LocalDateTime.now());

        // Act — intento de acceso cruzado (edge case de seguridad)
        Optional<AssetImage> result =
                repository.findByIdAndAssetId(imageOfOther.getId(), asset.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    // =========================================================
    //  findFirstByAssetIdAndIsPrimaryTrue
    // =========================================================

    @Test
    void should_returnPrimaryImage_when_assetHasPrimaryImage() {
        // Arrange
        persistImageWithUploadedAt(asset, false, LocalDateTime.now().minusHours(1));
        AssetImage primary = persistImageWithUploadedAt(asset, true, LocalDateTime.now());

        // Act
        Optional<AssetImage> result =
                repository.findFirstByAssetIdAndIsPrimaryTrue(asset.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(primary.getId());
        assertThat(result.get().getIsPrimary()).isTrue();
    }

    @Test
    void should_returnEmpty_when_noImageIsMarkedAsPrimary() {
        // Arrange — todas las imágenes son no-primarias
        persistImageWithUploadedAt(asset, false, LocalDateTime.now().minusHours(1));
        persistImageWithUploadedAt(asset, false, LocalDateTime.now());

        // Act
        Optional<AssetImage> result =
                repository.findFirstByAssetIdAndIsPrimaryTrue(asset.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    // =========================================================
    //  clearPrimaryByAssetId  (@Modifying)
    // =========================================================

    @Test
    void should_clearAllPrimaryFlags_when_assetHasPrimaryImages() {
        // Arrange
        persistImageWithUploadedAt(asset, true,  LocalDateTime.now().minusHours(2));
        persistImageWithUploadedAt(asset, true,  LocalDateTime.now().minusHours(1));
        persistImageWithUploadedAt(asset, false, LocalDateTime.now());
        // Act
        repository.clearPrimaryByAssetId(asset.getId());
        // Limpiamos el caché de 1er nivel para que las lecturas vayan a BD
        entityManager.flush();
        entityManager.clear();
        // Assert — ninguna imagen del asset debe tener isPrimary = true
        List<AssetImage> result =
                repository.findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(asset.getId());
        assertThat(result)
                .hasSize(3)
                .allMatch(img -> !img.getIsPrimary());
    }

    @Test
    void should_notAffectImagesOfOtherAssets_when_clearingPrimaryByAssetId() {
        // Arrange
        AssetImage primaryOfOther = persistImageWithUploadedAt(otherAsset, true, LocalDateTime.now());
        persistImageWithUploadedAt(asset, true, LocalDateTime.now());

        // Act
        repository.clearPrimaryByAssetId(asset.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert — la imagen de otherAsset debe conservar su isPrimary = true
        Optional<AssetImage> result =
                repository.findFirstByAssetIdAndIsPrimaryTrue(otherAsset.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(primaryOfOther.getId());
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private AssetImage persistImageWithUploadedAt(Asset asset,
                                                  boolean isPrimary,
                                                  LocalDateTime uploadedAt) {
        AssetImage image = buildImage(asset, isPrimary);
        entityManager.persistAndFlush(image); // @PrePersist establece uploadedAt = now()

        // JdbcTemplate maneja LocalDateTime nativamente y bypasea
        // tanto @PrePersist como updatable=false sin ambigüedad de tipos
        jdbcTemplate.update(
                "UPDATE asset_images SET uploaded_at = ? WHERE id = ?",
                uploadedAt,
                image.getId()
        );

        return image;
    }

    private AssetImage buildImage(Asset asset, boolean isPrimary) {
        AssetImage img = new AssetImage();
        img.setAsset(asset);
        img.setFilePath("/uploads/test-image.jpg");
        img.setFileName("test-image.jpg");
        img.setMimeType("image/jpeg");
        img.setIsPrimary(isPrimary);
        img.setUploadedBy(operatorUser);
        return img;
    }


}
