package mx.edu.unpa.inventory_backend.services.impl;

import mx.edu.unpa.inventory_backend.domains.Category;
import mx.edu.unpa.inventory_backend.dtos.category.request.CategoryRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.category.response.CategoryResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.mappers.CategoryMapper;
import mx.edu.unpa.inventory_backend.repositories.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Category buildCategory(Integer id, String name) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setIsActive(true);
        return c;
    }

    private CategoryRequestDTO buildRequest(String name, String description, Integer parentId) {
        return new CategoryRequestDTO(name, description, parentId);
    }

    private CategoryResponseDTO buildResponse(Integer id, String name, Integer parentId, String parentName) {
        return new CategoryResponseDTO(id, name, null, parentId, parentName, true);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // create
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_returnDto_when_createWithValidRequest() {
        // Arrange
        var request  = buildRequest("Equipo de Cómputo", "Computadoras y periféricos", null);
        var entity   = buildCategory(null, "Equipo de Cómputo");
        var saved    = buildCategory(1, "Equipo de Cómputo");
        var expected = buildResponse(1, "Equipo de Cómputo", null, null);

        when(categoryRepository.existsByName(request.name())).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(entity)).thenReturn(saved);
        when(categoryMapper.toDto(saved)).thenReturn(expected);

        // Act
        CategoryResponseDTO result = categoryService.create(request);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(categoryRepository).save(entity);
        // isActive se setea manualmente en el service — verificar que no depende del mapper
        assertThat(entity.getIsActive()).isTrue();
    }

    @Test
    void should_throwDuplicateResourceException_when_nameAlreadyExists() {
        // Arrange
        var request = buildRequest("Mobiliario", null, null);
        when(categoryRepository.existsByName(request.name())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Mobiliario");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void should_setParentFromRepository_when_parentIdIsProvided() {
        // Arrange
        var parent  = buildCategory(5, "Tecnología");
        var request = buildRequest("Laptops", null, 5);
        var entity  = buildCategory(null, "Laptops");
        var saved   = buildCategory(2, "Laptops");
        var expected = buildResponse(2, "Laptops", 5, "Tecnología");

        when(categoryRepository.existsByName(request.name())).thenReturn(false);
        when(categoryRepository.findByIdAndIsActiveTrue(5)).thenReturn(Optional.of(parent));
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(entity)).thenReturn(saved);
        when(categoryMapper.toDto(saved)).thenReturn(expected);

        // Act
        CategoryResponseDTO result = categoryService.create(request);

        // Assert
        assertThat(entity.getParent()).isEqualTo(parent);
        assertThat(result.parentId()).isEqualTo(5);
    }

    @Test
    void should_throwResourceNotFoundException_when_parentIdDoesNotExist() {
        // Arrange
        var request = buildRequest("Subcategoría Huérfana", null, 999);
        var entity  = buildCategory(null, "Subcategoría Huérfana");

        when(categoryRepository.existsByName(request.name())).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.findByIdAndIsActiveTrue(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void should_setNullParent_when_parentIdIsNull() {
        // Arrange — categoría raíz sin padre
        var request = buildRequest("Raíz", null, null);
        var entity  = buildCategory(null, "Raíz");
        var saved   = buildCategory(3, "Raíz");

        when(categoryRepository.existsByName(request.name())).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(entity)).thenReturn(saved);
        when(categoryMapper.toDto(saved)).thenReturn(buildResponse(3, "Raíz", null, null));

        // Act
        categoryService.create(request);

        // Assert — resolveParent(null) no debe consultar el repositorio
        verify(categoryRepository, never()).findByIdAndIsActiveTrue(null);
        assertThat(entity.getParent()).isNull();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // findAllActive
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_returnPageOfDtos_when_activeCategoriesExist() {
        // Arrange
        var pageable = PageRequest.of(0, 10);
        var category = buildCategory(1, "Mobiliario");
        var page     = new PageImpl<>(List.of(category), pageable, 1);
        var dto      = buildResponse(1, "Mobiliario", null, null);

        when(categoryRepository.findByIsActiveTrue(pageable)).thenReturn(page);
        when(categoryMapper.toDto(category)).thenReturn(dto);

        // Act
        Page<CategoryResponseDTO> result = categoryService.findAllActive(pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Mobiliario");
    }

    @Test
    void should_returnEmptyPage_when_noActiveCategoriesExist() {
        // Arrange
        var pageable = PageRequest.of(0, 10);
        when(categoryRepository.findByIsActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        Page<CategoryResponseDTO> result = categoryService.findAllActive(pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // search
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_delegateQueryToRepository_when_searchIsCalled() {
        // Arrange
        var pageable = PageRequest.of(0, 10);
        var category = buildCategory(1, "Equipo de Cómputo");
        var page     = new PageImpl<>(List.of(category), pageable, 1);
        var dto      = buildResponse(1, "Equipo de Cómputo", null, null);

        when(categoryRepository.searchActive("computo", pageable)).thenReturn(page);
        when(categoryMapper.toDto(category)).thenReturn(dto);

        // Act
        Page<CategoryResponseDTO> result = categoryService.search("computo", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(categoryRepository).searchActive("computo", pageable);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // findById
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_returnDto_when_categoryExistsAndIsActive() {
        // Arrange
        var category = buildCategory(1, "Mobiliario");
        var dto      = buildResponse(1, "Mobiliario", null, null);

        when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(category));
        when(categoryMapper.toDto(category)).thenReturn(dto);

        // Act
        CategoryResponseDTO result = categoryService.findById(1);

        // Assert
        assertThat(result.id()).isEqualTo(1);
        assertThat(result.name()).isEqualTo("Mobiliario");
    }

    @Test
    void should_throwResourceNotFoundException_when_categoryNotFound() {
        // Arrange
        when(categoryRepository.findByIdAndIsActiveTrue(99)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.findById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void should_throwResourceNotFoundException_when_categoryIsInactive() {
        // findByIdAndIsActiveTrue ya filtra inactivas en la BD — si devuelve empty,
        // el service lanza ResourceNotFoundException sin distinguir si existe o no.
        when(categoryRepository.findByIdAndIsActiveTrue(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(5))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // update
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_returnUpdatedDto_when_updateWithSameName() {
        // Arrange — mismo nombre (case-insensitive): no debe verificar duplicado
        var existing = buildCategory(1, "Mobiliario");
        var request  = buildRequest("mobiliario", "Descripción nueva", null);
        var saved    = buildCategory(1, "mobiliario");
        var expected = buildResponse(1, "mobiliario", null, null);

        when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(existing));
        // existsByNameAndIdNot NO debe ser llamado cuando el nombre no cambió
        when(categoryRepository.save(existing)).thenReturn(saved);
        when(categoryMapper.toDto(saved)).thenReturn(expected);

        // Act
        CategoryResponseDTO result = categoryService.update(1, request);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(categoryRepository, never()).existsByNameAndIdNot(any(), any());
    }

    @Test
    void should_throwDuplicateResourceException_when_newNameAlreadyExistsOnAnotherCategory() {
        // Arrange — nombre cambia y ya existe en otra categoría
        var existing = buildCategory(1, "Mobiliario");
        var request  = buildRequest("Equipo de Cómputo", null, null);

        when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndIdNot("Equipo de Cómputo", 1)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.update(1, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Equipo de Cómputo");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void should_throwIllegalArgumentException_when_categorySetAsItsOwnParent() {
        // Arrange — parentId apunta al mismo id que la categoría
        var existing = buildCategory(1, "Mobiliario");
        var request  = buildRequest("Mobiliario Nuevo", null, 1); // parentId == id

        when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndIdNot("Mobiliario Nuevo", 1)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.update(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("propia categoría padre");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void should_updateParent_when_validParentIdIsProvided() {
        // Arrange
        var existing  = buildCategory(1, "Laptops");
        var newParent = buildCategory(10, "Tecnología");
        var request   = buildRequest("Laptops Pro", null, 10);
        var saved     = buildCategory(1, "Laptops Pro");
        var expected  = buildResponse(1, "Laptops Pro", 10, "Tecnología");

        when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndIdNot("Laptops Pro", 1)).thenReturn(false);
        when(categoryRepository.findByIdAndIsActiveTrue(10)).thenReturn(Optional.of(newParent));
        when(categoryRepository.save(existing)).thenReturn(saved);
        when(categoryMapper.toDto(saved)).thenReturn(expected);

        // Act
        CategoryResponseDTO result = categoryService.update(1, request);

        // Assert
        assertThat(existing.getParent()).isEqualTo(newParent);
        assertThat(result.parentId()).isEqualTo(10);
    }

    @Test
    void should_throwResourceNotFoundException_when_updatingNonExistentCategory() {
        // Arrange
        var request = buildRequest("Nombre", null, null);
        when(categoryRepository.findByIdAndIsActiveTrue(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.update(999, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // NOTA: edge case documentado — el service usa equalsIgnoreCase para detectar
    // si el nombre cambió, pero existsByNameAndIdNot hace comparación case-sensitive
    // en la BD. Cambiar "Computo" → "computo" saltará la validación de duplicado
    // aunque ya exista. Pendiente alinear con el repositorio.
    @Test
    void should_skipDuplicateCheck_when_nameChangesOnlyCasing() {
        // Arrange
        var existing = buildCategory(1, "Computo");
        var request  = buildRequest("computo", null, null); // solo cambia capitalización

        when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(existing)).thenReturn(existing);
        when(categoryMapper.toDto(existing)).thenReturn(buildResponse(1, "computo", null, null));

        // Act — no debe lanzar excepción ni consultar existsByNameAndIdNot
        categoryService.update(1, request);

        // Assert
        verify(categoryRepository, never()).existsByNameAndIdNot(any(), any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // deactivate
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_setIsActiveFalse_when_deactivatingExistingCategory() {
        // Arrange
        var category = buildCategory(1, "Mobiliario");
        when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        // Act
        categoryService.deactivate(1);

        // Assert
        assertThat(category.getIsActive()).isFalse();
        verify(categoryRepository).save(category);
    }

    @Test
    void should_throwResourceNotFoundException_when_deactivatingNonExistentCategory() {
        // Arrange
        when(categoryRepository.findByIdAndIsActiveTrue(404)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.deactivate(404))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");

        verify(categoryRepository, never()).save(any());
    }
}