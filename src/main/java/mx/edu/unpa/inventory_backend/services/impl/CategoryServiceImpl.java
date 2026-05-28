package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.Category;
import mx.edu.unpa.inventory_backend.dtos.category.request.CategoryRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.category.response.CategoryResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.mappers.CategoryMapper;
import mx.edu.unpa.inventory_backend.repositories.CategoryRepository;
import mx.edu.unpa.inventory_backend.services.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper      categoryMapper;

    // ── Crear ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException(
                    "Ya existe una categoría con el nombre: " + request.name());
        }

        Category category = categoryMapper.toEntity(request);
        category.setIsActive(true);
        category.setParent(resolveParent(request.parentId()));

        return categoryMapper.toDto(categoryRepository.save(category));
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponseDTO> findAllActive(Pageable pageable) {
        return categoryRepository
                .findByIsActiveTrue(pageable)
                .map(categoryMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponseDTO> search(String q, Pageable pageable) {
        return categoryRepository
                .searchActive(q, pageable)
                .map(categoryMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO findById(Integer id) {
        return categoryMapper.toDto(getOrThrow(id));
    }

    // ── Actualizar ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CategoryResponseDTO update(Integer id, CategoryRequestDTO request) {
        Category category = getOrThrow(id);

        // Valida unicidad de nombre solo si cambió
        if (!category.getName().equalsIgnoreCase(request.name())
                && categoryRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException(
                    "Ya existe una categoría con el nombre: " + request.name());
        }

        // Previene que una categoría sea su propio padre
        if (request.parentId() != null && request.parentId().equals(id)) {
            throw new IllegalArgumentException(
                    "Una categoría no puede ser su propia categoría padre");
        }

        categoryMapper.updateEntityFromDto(request, category);
        category.setParent(resolveParent(request.parentId()));

        return categoryMapper.toDto(categoryRepository.save(category));
    }

    // ── Baja lógica ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deactivate(Integer id) {
        Category category = getOrThrow(id);
        category.setIsActive(false);
        categoryRepository.save(category);
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private Category getOrThrow(Integer id) {
        return categoryRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoría no encontrada con id: " + id));
    }

    /**
     * Resuelve la categoría padre a partir del ID.
     * Retorna null si parentId es null (categoría raíz).
     */
    private Category resolveParent(Integer parentId) {
        if (parentId == null) return null;
        return categoryRepository.findByIdAndIsActiveTrue(parentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoría padre no encontrada con id: " + parentId));
    }
}
