package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetAssignmentRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetAssignmentResponseDTO;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.services.AssetAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/assignments")
@RequiredArgsConstructor
public class AssetAssignmentController {

    private final AssetAssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<AssetAssignmentResponseDTO> createAssignment(
            @Valid @RequestBody AssetAssignmentRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.assignAsset(request, currentUser.id()));
    }
}
