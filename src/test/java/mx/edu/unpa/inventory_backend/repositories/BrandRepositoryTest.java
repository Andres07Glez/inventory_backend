package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Brand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BrandRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private BrandRepository brandRepository;

    // -------------------------------------------------------------------------
    // findByIsActiveTrueOrderByNameAsc
    // -------------------------------------------------------------------------

    @Test
    void should_returnOnlyActiveBrands_when_mixedActiveAndInactiveExist() {
        // Arrange
        // "Dell" (isActive=true) ya existe en el contexto vía BaseRepositoryTest
        entityManager.persistAndFlush(buildBrand("Apple"));   // isActive=true
        Brand hpInactive = buildBrand("HP");
        hpInactive.setIsActive(false);
        entityManager.persistAndFlush(hpInactive);

        // Act
        List<Brand> result = brandRepository.findByIsActiveTrueOrderByNameAsc();

        // Assert
        assertThat(result)
                .extracting(Brand::getName)
                .containsExactly("Apple", "Dell")   // orden alfabético, solo activos
                .doesNotContain("HP");
    }

    @Test
    void should_returnEmptyList_when_noActiveBrandsExist() {
        // Arrange
        // El "Dell" heredado está activo — lo desactivamos para aislar el test
        brand.setIsActive(false);
        entityManager.persistAndFlush(brand);

        Brand inactive = buildBrand("Lenovo");
        inactive.setIsActive(false);
        entityManager.persistAndFlush(inactive);

        // Act
        List<Brand> result = brandRepository.findByIsActiveTrueOrderByNameAsc();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnBrandsAlphabetically_when_multipleActiveBrandsExist() {
        // Arrange
        // "Dell" ya está en el contexto (BaseRepositoryTest)
        entityManager.persistAndFlush(buildBrand("Zebra"));
        entityManager.persistAndFlush(buildBrand("Acer"));

        // Act
        List<Brand> result = brandRepository.findByIsActiveTrueOrderByNameAsc();

        // Assert — verificamos que el ordenamiento sea correcto (ASC)
        assertThat(result)
                .extracting(Brand::getName)
                .containsExactly("Acer", "Dell", "Zebra");
    }

    // -------------------------------------------------------------------------
    // findByNameIgnoreCase
    // -------------------------------------------------------------------------

    @Test
    void should_findBrand_when_nameMatchesExactCase() {
        // Arrange — "Dell" ya existe vía BaseRepositoryTest

        // Act
        Optional<Brand> result = brandRepository.findByNameIgnoreCase("Dell");

        // Assert
        assertThat(result)
                .isPresent()
                .get()
                .extracting(Brand::getName)
                .isEqualTo("Dell");
    }

    @Test
    void should_findBrand_when_nameProvidedInLowerCase() {
        // Arrange — "Dell" ya existe vía BaseRepositoryTest

        // Act
        Optional<Brand> result = brandRepository.findByNameIgnoreCase("dell");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Dell");
    }

    @Test
    void should_findBrand_when_nameProvidedInUpperCase() {
        // Act
        Optional<Brand> result = brandRepository.findByNameIgnoreCase("DELL");

        // Assert
        assertThat(result).isPresent();
    }

    @Test
    void should_findBrand_when_nameProvidedInMixedCase() {
        // Act
        Optional<Brand> result = brandRepository.findByNameIgnoreCase("dElL");

        // Assert
        assertThat(result).isPresent();
    }

    @Test
    void should_returnEmpty_when_nameDoesNotExist() {
        // Act
        Optional<Brand> result = brandRepository.findByNameIgnoreCase("NonExistentBrand");

        // Assert
        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // existsByNameIgnoreCase
    // -------------------------------------------------------------------------

    @Test
    void should_returnTrue_when_brandExistsWithExactCase() {
        // Arrange — "Dell" ya existe vía BaseRepositoryTest

        // Act
        boolean exists = brandRepository.existsByNameIgnoreCase("Dell");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void should_returnTrue_when_brandExistsWithDifferentCase() {
        // Act
        boolean existsLower = brandRepository.existsByNameIgnoreCase("dell");
        boolean existsUpper = brandRepository.existsByNameIgnoreCase("DELL");
        boolean existsMixed = brandRepository.existsByNameIgnoreCase("dElL");

        // Assert
        assertThat(existsLower).isTrue();
        assertThat(existsUpper).isTrue();
        assertThat(existsMixed).isTrue();
    }

    @Test
    void should_returnFalse_when_brandDoesNotExist() {
        // Act
        boolean exists = brandRepository.existsByNameIgnoreCase("Motorola");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void should_returnFalse_when_nameIsPartialMatch() {
        // Edge case: "Del" no debe hacer match con "Dell"
        // Act
        boolean exists = brandRepository.existsByNameIgnoreCase("Del");

        // Assert
        assertThat(exists).isFalse();
    }
}