package mx.edu.unpa.inventory_backend.services.impl;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.*;
import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetAssignmentRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetAssignmentResponseDTO;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
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
    private final AssetRepository           assetRepository;
    private final GuardianRepository        guardianRepository;
    private final AssetAssignmentMapper     mapper;
    private final UserRepository            userRepository;

    @Override
    @Transactional
    public AssetAssignmentResponseDTO assignAsset(AssetAssignmentRequestDTO request) {

        // 1. Validar el bien
        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bien no encontrado con id: " + request.assetId()));

        if (asset.getLifecycleStatus() == LifecycleStatus.DECOMMISSIONED) {
            throw new IllegalStateException("No se puede asignar un bien dado de baja");
        }

        // 2. Validar el resguardante
        Guardian guardian = guardianRepository.findById(request.guardianId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resguardante no encontrado con id: " + request.guardianId()));

        // 3. La ubicación se hereda del resguardante.
        //    Si el resguardante no tiene ubicación base registrada se lanza una excepción
        //    clara para que el administrador la configure antes de asignar bienes.
        Location location = guardian.getLocation();
        if (location == null) {
            throw new IllegalStateException(
                    "El resguardante '" + guardian.getFullName() +
                            "' no tiene una ubicación base registrada. " +
                            "Actualice el resguardante antes de asignarle bienes.");
        }

        // 4. Validar usuario que realiza la asignación
        User assignedBy = userRepository.findById(request.assignedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario asignador no encontrado con id: " + request.assignedBy()));

        // 5. Cerrar la asignación activa anterior si existe (fix bug duplicados)
        assignmentRepository.findActiveByAssetId(asset.getId())
                .ifPresent(existing -> {
                    existing.setReturnedAt(LocalDateTime.now());
                    assignmentRepository.save(existing);
                });

        // 6. Crear la nueva asignación con la ubicación heredada del resguardante
        AssetAssignment assignment = new AssetAssignment();
        assignment.setAsset(asset);
        assignment.setGuardian(guardian);
        assignment.setLocation(location);
        assignment.setAssignedBy(assignedBy);
        assignment.setNotes(request.notes());
        assignment.setAssignedAt(LocalDateTime.now());

        // 7. Sincronizar el bien: ubicación y estado de ciclo de vida
        asset.setLifecycleStatus(LifecycleStatus.ASSIGNED);
        asset.setLocation(location);
        assetRepository.save(asset);

        // 8. Guardar y retornar
        return mapper.toDto(assignmentRepository.save(assignment));
    }
}
