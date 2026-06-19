package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    // Pageable genérico reutilizable en todos los tests de esta clase
    private static final Pageable PAGE = PageRequest.of(0, 10);

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers locales
    // ─────────────────────────────────────────────────────────────────────────

    private Category buildRootCategory(String name) {
        Category c = new Category();
        c.setName(name);
        c.setIsActive(true);
        return c;
    }

    private Category buildSubCategory(String name, Category parent) {
        Category c = new Category();
        c.setName(name);
        c.setParent(parent);
        c.setIsActive(true);
        return c;
    }

    private Category buildInactiveCategory(String name) {
        Category c = buildRootCategory(name);
        c.setIsActive(false);
        return c;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByIdAndIsActiveTrue
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_findCategory_when_idExistsAndIsActive() {
        // Arrange — "Equipos de Cómputo" (isActive=true) ya existe vía BaseRepositoryTest

        // Act
        Optional<Category> result = categoryRepository.findByIdAndIsActiveTrue(category.getId());

        // Assert
        assertThat(result)
                .isPresent()
                .get()
                .extracting(Category::getName)
                .isEqualTo("Equipos de Cómputo");
    }

    @Test
    void should_returnEmpty_when_idExistsButCategoryIsInactive() {
        // Arrange
        Category inactive = entityManager.persistAndFlush(buildInactiveCategory("Inactiva"));

        // Act
        Optional<Category> result = categoryRepository.findByIdAndIsActiveTrue(inactive.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_idDoesNotExist() {
        // Act
        Optional<Category> result = categoryRepository.findByIdAndIsActiveTrue(Integer.MAX_VALUE);

        // Assert
        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // existsByName
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnTrue_when_categoryWithNameExists() {
        // Arrange — "Equipos de Cómputo" ya existe vía BaseRepositoryTest

        // Act + Assert
        assertThat(categoryRepository.existsByName("Equipos de Cómputo")).isTrue();
    }

    @Test
    void should_returnFalse_when_categoryWithNameDoesNotExist() {
        // Act + Assert
        assertThat(categoryRepository.existsByName("Categoría Inexistente")).isFalse();
    }

    @Test
    void should_returnTrue_when_inactiveCategoryWithNameExists() {
        // Edge case: existsByName NO filtra por isActive — verifica existencia pura.
        // Sirve para validar unicidad de nombre a nivel de tabla, no de estado activo.
        // Arrange
        entityManager.persistAndFlush(buildInactiveCategory("Categoría Inactiva"));

        // Act + Assert
        assertThat(categoryRepository.existsByName("Categoría Inactiva")).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // existsByNameAndIdNot
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnFalse_when_nameMatchesOwnRecord() {
        // Edge case principal: al editar una categoría, su propio nombre
        // no debe tratarse como conflicto.
        // Act
        boolean conflict = categoryRepository.existsByNameAndIdNot(
                "Equipos de Cómputo", category.getId()
        );

        // Assert
        assertThat(conflict).isFalse();
    }

    @Test
    void should_returnTrue_when_nameMatchesDifferentRecord() {
        // Arrange
        entityManager.persistAndFlush(buildRootCategory("Mobiliario"));

        // Act — intentamos "usar" el nombre "Mobiliario" desde el id de otra categoría
        boolean conflict = categoryRepository.existsByNameAndIdNot(
                "Mobiliario", category.getId()
        );

        // Assert
        assertThat(conflict).isTrue();
    }

    @Test
    void should_returnFalse_when_nameDoesNotExistAtAll() {
        // Act
        boolean conflict = categoryRepository.existsByNameAndIdNot(
                "Nombre Totalmente Nuevo", category.getId()
        );

        // Assert
        assertThat(conflict).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByIsActiveTrue (paginación)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnOnlyActiveCategories_when_mixedActiveAndInactiveExist() {
        // Arrange — "Equipos de Cómputo" (isActive=true) ya existe vía BaseRepositoryTest
        entityManager.persistAndFlush(buildRootCategory("Mobiliario"));
        entityManager.persistAndFlush(buildInactiveCategory("Deprecada"));

        // Act
        Page<Category> result = categoryRepository.findByIsActiveTrue(PAGE);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Equipos de Cómputo", "Mobiliario")
                .doesNotContain("Deprecada");
    }

    @Test
    void should_respectPagination_when_multipleActiveCategoriesExist() {
        // Arrange — total activas: 3 ("Equipos de Cómputo" + "Mobiliario" + "Software")
        entityManager.persistAndFlush(buildRootCategory("Mobiliario"));
        entityManager.persistAndFlush(buildRootCategory("Software"));

        Pageable firstPage = PageRequest.of(0, 2);

        // Act
        Page<Category> result = categoryRepository.findByIsActiveTrue(firstPage);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // @Query — findRootCategories
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_returnOnlyRootCategories_when_subCategoriesExist() {
        // Arrange — "Equipos de Cómputo" es raíz y ya existe vía BaseRepositoryTest
        Category mobiliario = entityManager.persistAndFlush(buildRootCategory("Mobiliario"));
        entityManager.persistAndFlush(buildSubCategory("Sillas", mobiliario));
        entityManager.persistAndFlush(buildSubCategory("Escritorios", mobiliario));

        // Act
        Page<Category> result = categoryRepository.findRootCategories(PAGE);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Equipos de Cómputo", "Mobiliario")
                .doesNotContain("Sillas", "Escritorios");
    }

    @Test
    void should_excludeInactiveRootCategories_when_someRootsAreInactive() {
        // Arrange
        entityManager.persistAndFlush(buildInactiveCategory("Raíz Inactiva"));

        // Act
        Page<Category> result = categoryRepository.findRootCategories(PAGE);

        // Assert — solo "Equipos de Cómputo" es raíz activa
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(Category::getName)
                .containsExactly("Equipos de Cómputo");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // @Query — searchActive
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void should_findCategory_when_queryMatchesCategoryNameDirectly() {
        // Arrange — "Equipos de Cómputo" ya existe.
        // NOTA: se usa "Equipos" (sin tilde) porque H2 no normaliza caracteres
        // acentuados — LOWER('ó') != LOWER('o') en H2, a diferencia de MariaDB.

        // Act
        Page<Category> result = categoryRepository.searchActive("Equipos", PAGE);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Equipos de Cómputo");
    }

    @Test
    void should_beCaseInsensitive_when_queryUsesUpperCase() {
        // Arrange — "Equipos de Cómputo" ya existe

        // Act
        Page<Category> result = categoryRepository.searchActive("EQUIPOS", PAGE);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void should_returnEmpty_when_queryMatchesNoActiveCategory() {
        // Act
        Page<Category> result = categoryRepository.searchActive("xyzNoExiste", PAGE);

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void should_excludeInactiveCategories_when_queryMatchesInactiveName() {
        // Arrange
        entityManager.persistAndFlush(buildInactiveCategory("Ultravioleta Especial"));

        // Act — "ultravioleta" solo hace match en la categoría inactiva
        Page<Category> result = categoryRepository.searchActive("ultravioleta", PAGE);

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void should_returnParentAndChild_when_queryMatchesChildName() {
        // El query evalúa TODAS las categorías activas contra ambas condiciones del OR:
        //   1. c.name LIKE '%q%'      → match directo por nombre
        //   2. c.id IN (subconsulta)  → match como padre de un hijo que hace match
        //
        // Consecuencia: si un hijo hace match, aparecen DOS resultados:
        //   - El hijo mismo (cumple condición 1)
        //   - Su padre   (cumple condición 2)
        //
        // Arrange
        Category mobiliario = entityManager.persistAndFlush(buildRootCategory("Mobiliario"));
        entityManager.persistAndFlush(buildSubCategory("Silla Xenon Pro", mobiliario));

        // Act — "xenon" está solo en el hijo; "Mobiliario" no contiene "xenon"
        Page<Category> result = categoryRepository.searchActive("xenon", PAGE);

        // Assert — hijo (match directo) + padre (vía subconsulta)
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Mobiliario", "Silla Xenon Pro");
    }

    @Test
    void should_includeParentExactlyOnce_when_multipleChildrenMatchQuery() {
        // Edge case: varios hijos del mismo padre hacen match.
        // Los hijos aparecen como match directo (condición 1).
        // El padre aparece vía subconsulta (condición 2).
        // El padre debe aparecer UNA SOLA VEZ aunque múltiples hijos lo referencien
        // — el IN de la subconsulta es idempotente a nivel SQL.
        //
        // Arrange — "zeta" no está en "Equipos de Cómputo" ni en "Mobiliario"
        Category mobiliario = entityManager.persistAndFlush(buildRootCategory("Mobiliario"));
        entityManager.persistAndFlush(buildSubCategory("Silla Zeta Ergonomica", mobiliario));
        entityManager.persistAndFlush(buildSubCategory("Mesa Zeta Standing", mobiliario));

        // Act — "zeta" hace match en ambos hijos directamente, y en el padre vía subconsulta
        Page<Category> result = categoryRepository.searchActive("zeta", PAGE);

        // Assert — 3 resultados distintos: padre (1 vez) + 2 hijos
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Mobiliario", "Silla Zeta Ergonomica", "Mesa Zeta Standing");
    }
}