package mx.edu.unpa.inventory_backend.services.impl;


import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.*;
import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetAssignmentRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetAssignmentResponseDTO;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.mappers.AssetAssignmentMapper;
import mx.edu.unpa.inventory_backend.repositories.*;
import mx.edu.unpa.inventory_backend.services.AssetAssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AssetAssignmentServiceImpl implements AssetAssignmentService {

    private final AssetAssignmentRepository assignmentRepository;
    private final AssetRepository assetRepository;
    private final GuardianRepository guardianRepository; // Inyectado correctamente
    private final LocationRepository locationRepository;
    private final AssetAssignmentMapper mapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AssetAssignmentResponseDTO assignAsset(AssetAssignmentRequestDTO request) {

        // 1. Validar el bien
        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow(() -> new RuntimeException("Bien no encontrado"));

        if (asset.getLifecycleStatus() == LifecycleStatus.DECOMMISSIONED) {
            throw new IllegalStateException("No se puede asignar un bien dado de baja");
        }

        // 2. Validar el Guardián usando tu nuevo repositorio
        Guardian guardian = guardianRepository.findById(request.guardianId())
                .orElseThrow(() -> new RuntimeException("Resguardante no encontrado"));

        // 3. Validar la ubicación
        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));
        // 2. Buscamos al usuario que está haciendo la asignación
        User assignedBy = userRepository.findById(request.assignedBy())
                .orElseThrow(() -> new RuntimeException("Usuario asignador no encontrado"));

        // 4. Crear la asignación con la entidad de tu compañero
        AssetAssignment assignment = new AssetAssignment();
        assignment.setAsset(asset);
        assignment.setGuardian(guardian);
        assignment.setLocation(location);
        assignment.setAssignedBy(assignedBy);
        assignment.setNotes(request.notes());
        assignment.setAssignedAt(LocalDateTime.now()); // Llenado manual obligatorio

        // 5. Actualizar el estado físico del bien
        asset.setLifecycleStatus(LifecycleStatus.ASSIGNED);
        asset.setLocation(location);
        assetRepository.save(asset);

        // 6. Guardar y retornar
        AssetAssignment savedAssignment = assignmentRepository.save(assignment);

        return mapper.toDto(savedAssignment);
    }
}
