package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.user.request.CreateUserRequest;
import mx.edu.unpa.inventory_backend.dtos.user.request.UpdateUserRoleRequest;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserDetailResponse;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserSummaryResponse;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.services.UserManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")  // Doble protección junto con SecurityConfig
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    public ResponseEntity<Page<UserSummaryResponse>> findAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(userManagementService.findAll(search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDetailResponse>> create(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(userManagementService.create(request)));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(
                userManagementService.updateRole(id, request, currentUser.id())
        ));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserDetailResponse>> toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(
                userManagementService.toggleStatus(id, currentUser.id())
        ));
    }
}
