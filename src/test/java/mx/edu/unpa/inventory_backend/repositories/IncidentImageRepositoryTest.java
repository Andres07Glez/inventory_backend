package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.Incident;
import mx.edu.unpa.inventory_backend.domains.IncidentImage;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentImageRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private IncidentImageRepository incidentImageRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers locales
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Persiste un Asset con todos sus campos obligatorios (delega en BaseRepositoryTest).
     */
    private Asset persistAsset(String inventoryNumber) {
        return entityManager.persistAndFlush(buildAsset(inventoryNumber));
    }

    /**
     * Construye y persiste una Incident mínima válida.
     * conditionAtIncident es NOT NULL — siempre requerido.
     */
    private Incident persistIncident(Asset asset) {
        Incident i = new Incident();
        i.setAsset(asset);
        i.setIncidentDate(LocalDate.now());
        i.setDescription("Daño por caída");
        i.setConditionAtIncident(ConditionStatus.BAD);
        i.setCreatedBy(operatorUser);
        return entityManager.persistAndFlush(i);
    }

    /**
     * Construye una IncidentImage sin persistir.
     * El caller elige cuándo y cómo persistirla (para poder controlar uploadedAt).
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
    // findByIncidentIdOrdered
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnImages_when_incidentHasImages() {
        // Arrange
        Asset asset = persistAsset("INV-001");
        Incident incident = persistIncident(asset);
        entityManager.persistAndFlush(buildImage(incident, "foto1.jpg"));
        entityManager.persistAndFlush(buildImage(incident, "foto2.jpg"));
        entityManager.clear();

        // Act
        List<IncidentImage> result = incidentImageRepository
                .findByIncidentIdOrdered(incident.getId());

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(IncidentImage::getFileName)
                .containsExactlyInAnyOrder("foto1.jpg", "foto2.jpg");
    }

    @Test
    void should_returnEmpty_when_incidentHasNoImages() {
        // Arrange
        Asset asset = persistAsset("INV-002");
        Incident incident = persistIncident(asset);

        // Act
        List<IncidentImage> result = incidentImageRepository
                .findByIncidentIdOrdered(incident.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_incidentIdDoesNotExist() {
        // Act
        List<IncidentImage> result = incidentImageRepository
                .findByIncidentIdOrdered(Long.MAX_VALUE);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnOnlyImagesOfGivenIncident_when_multipleIncidentsHaveImages() {
        // Verifica que el filtro por incidentId funciona correctamente
        // y no devuelve imágenes de otras incidencias.
        // Arrange
        Asset asset = persistAsset("INV-003");
        Incident incident1 = persistIncident(asset);
        Incident incident2 = persistIncident(asset);

        entityManager.persistAndFlush(buildImage(incident1, "inc1-foto.jpg"));
        entityManager.persistAndFlush(buildImage(incident2, "inc2-foto.jpg"));
        entityManager.clear();

        // Act
        List<IncidentImage> result = incidentImageRepository
                .findByIncidentIdOrdered(incident1.getId());

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileName()).isEqualTo("inc1-foto.jpg");
    }

    @Test
    void should_returnImagesOrderedByUploadedAtAsc() {
        // uploadedAt se inicializa inline (= LocalDateTime.now()) — no vía @PrePersist.
        // En tests rápidos, las imágenes persistidas en el mismo instante tendrían
        // el mismo timestamp, haciendo el orden no determinístico.
        // Solución (Problema 2 del CONTEXTO): JdbcTemplate fuerza valores distintos
        // en BD, luego entityManager.clear() invalida el caché de primer nivel.

        // Arrange
        Asset asset = persistAsset("INV-004");
        Incident incident = persistIncident(asset);

        IncidentImage first  = entityManager.persistAndFlush(buildImage(incident, "primera.jpg"));
        IncidentImage second = entityManager.persistAndFlush(buildImage(incident, "segunda.jpg"));
        IncidentImage third  = entityManager.persistAndFlush(buildImage(incident, "tercera.jpg"));

        jdbcTemplate.update(
                "UPDATE incident_images SET uploaded_at = ? WHERE id = ?",
                LocalDateTime.now().minusHours(2), first.getId()
        );
        jdbcTemplate.update(
                "UPDATE incident_images SET uploaded_at = ? WHERE id = ?",
                LocalDateTime.now().minusHours(1), second.getId()
        );
        jdbcTemplate.update(
                "UPDATE incident_images SET uploaded_at = ? WHERE id = ?",
                LocalDateTime.now(), third.getId()
        );
        entityManager.clear();

        // Act
        List<IncidentImage> result = incidentImageRepository
                .findByIncidentIdOrdered(incident.getId());

        // Assert — ORDER BY uploadedAt ASC: la más antigua debe ser la primera
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getFileName()).isEqualTo("primera.jpg");
        assertThat(result.get(1).getFileName()).isEqualTo("segunda.jpg");
        assertThat(result.get(2).getFileName()).isEqualTo("tercera.jpg");
    }

    @Test
    void should_haveUploadedByLoaded_when_imagesAreReturned() {
        // Verifica que el JOIN FETCH img.uploadedBy del query evita LazyInitializationException
        // al acceder a uploadedBy fuera de una sesión JPA abierta.
        // Arrange
        Asset asset = persistAsset("INV-005");
        Incident incident = persistIncident(asset);
        entityManager.persistAndFlush(buildImage(incident, "evidencia.jpg"));
        entityManager.clear();

        // Act
        List<IncidentImage> result = incidentImageRepository
                .findByIncidentIdOrdered(incident.getId());

        // Assert — acceder a uploadedBy no lanza LazyInitializationException
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUploadedBy()).isNotNull();
        assertThat(result.get(0).getUploadedBy().getUsername()).isEqualTo("operador01");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByIdAndIncidentId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_findImage_when_idAndIncidentIdBothMatch() {
        // Arrange
        Asset asset = persistAsset("INV-006");
        Incident incident = persistIncident(asset);
        IncidentImage image = entityManager.persistAndFlush(buildImage(incident, "match.jpg"));

        // Act
        Optional<IncidentImage> result = incidentImageRepository
                .findByIdAndIncidentId(image.getId(), incident.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getFileName()).isEqualTo("match.jpg");
    }

    @Test
    void should_returnEmpty_when_imageIdExistsButBelongsToDifferentIncident() {
        // Edge case de seguridad: un usuario no debe poder eliminar/acceder a una imagen
        // de otra incidencia pasando un incidentId incorrecto.
        // Arrange
        Asset asset = persistAsset("INV-007");
        Incident incident1 = persistIncident(asset);
        Incident incident2 = persistIncident(asset);

        IncidentImage imageOfIncident1 = entityManager
                .persistAndFlush(buildImage(incident1, "pertenece-a-inc1.jpg"));

        // Act — se pasa el id correcto de la imagen pero el incidentId equivocado
        Optional<IncidentImage> result = incidentImageRepository
                .findByIdAndIncidentId(imageOfIncident1.getId(), incident2.getId());

        // Assert — debe ser empty, no retornar la imagen de otra incidencia
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_imageIdDoesNotExist() {
        // Arrange
        Asset asset = persistAsset("INV-008");
        Incident incident = persistIncident(asset);

        // Act
        Optional<IncidentImage> result = incidentImageRepository
                .findByIdAndIncidentId(Long.MAX_VALUE, incident.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // countByIncidentId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnCorrectCount_when_incidentHasImages() {
        // Arrange
        Asset asset = persistAsset("INV-009");
        Incident incident = persistIncident(asset);
        entityManager.persistAndFlush(buildImage(incident, "img1.jpg"));
        entityManager.persistAndFlush(buildImage(incident, "img2.jpg"));
        entityManager.persistAndFlush(buildImage(incident, "img3.jpg"));

        // Act
        long count = incidentImageRepository.countByIncidentId(incident.getId());

        // Assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    void should_returnZero_when_incidentHasNoImages() {
        // Arrange
        Asset asset = persistAsset("INV-010");
        Incident incident = persistIncident(asset);

        // Act
        long count = incidentImageRepository.countByIncidentId(incident.getId());

        // Assert
        assertThat(count).isZero();
    }

    @Test
    void should_countOnlyImagesOfGivenIncident_when_multipleIncidentsHaveImages() {
        // Verifica que el count no suma imágenes de otras incidencias.
        // Arrange
        Asset asset = persistAsset("INV-011");
        Incident incident1 = persistIncident(asset);
        Incident incident2 = persistIncident(asset);

        entityManager.persistAndFlush(buildImage(incident1, "a.jpg"));
        entityManager.persistAndFlush(buildImage(incident1, "b.jpg"));
        entityManager.persistAndFlush(buildImage(incident2, "c.jpg")); // no debe sumarse

        // Act
        long count = incidentImageRepository.countByIncidentId(incident1.getId());

        // Assert
        assertThat(count).isEqualTo(2);
    }
}