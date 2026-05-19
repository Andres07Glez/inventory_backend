package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.user.request.CreateUserRequest;
import mx.edu.unpa.inventory_backend.dtos.user.request.UpdateUserRoleRequest;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserDetailResponse;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserManagementService {
    Page<UserSummaryResponse> findAll(String search, Pageable pageable);
    UserDetailResponse findById(Long id);
    UserDetailResponse create(CreateUserRequest request);
    UserDetailResponse updateRole(Long targetUserId, UpdateUserRoleRequest request, Long currentUserId);
    UserDetailResponse toggleStatus(Long targetUserId, Long currentUserId);
}
