package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.unpa.inventory_backend.domains.Brand;
import mx.edu.unpa.inventory_backend.dtos.brand.request.BrandRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.brand.response.BrandResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.BrandRepository;
import mx.edu.unpa.inventory_backend.services.BrandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    @Override
    public List<BrandResponseDTO> getAllActive() {
        return brandRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public BrandResponseDTO create(BrandRequestDTO request) {
        String name = request.getName().trim();

        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException(
                    "Ya existe una marca con el nombre: " + name);
        }

        Brand brand = new Brand();
        brand.setName(name);
        brand.setIsActive(true);

        Brand saved = brandRepository.save(brand);
        log.info("Marca creada: {}", saved.getName());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public BrandResponseDTO update(Integer id, BrandRequestDTO request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Marca no encontrada: " + id));

        String newName = request.getName().trim();

        // Valida duplicado solo si el nombre cambió
        if (!brand.getName().equalsIgnoreCase(newName)
                && brandRepository.existsByNameIgnoreCase(newName)) {
            throw new DuplicateResourceException(
                    "Ya existe una marca con el nombre: " + newName);
        }

        brand.setName(newName);
        log.info("Marca actualizada id={} → {}", id, newName);
        return toDTO(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Marca no encontrada: " + id));

        // Soft delete — no borra el registro, solo lo desactiva
        brand.setIsActive(false);
        brandRepository.save(brand);
        log.info("Marca desactivada id={}", id);
    }

    private BrandResponseDTO toDTO(Brand b) {
        return new BrandResponseDTO(b.getId(), b.getName(), b.getIsActive());
    }

}
