package mx.edu.unpa.inventory_backend.controllers;

import tools.jackson.databind.ObjectMapper;
import mx.edu.unpa.inventory_backend.dtos.guardian.response.GuardianSummary;
import mx.edu.unpa.inventory_backend.dtos.user.request.CreateUserRequest;
import mx.edu.unpa.inventory_backend.dtos.user.request.UpdateUserRoleRequest;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserDetailResponse;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserSummaryResponse;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = {UserManagementController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserManagementControllerTest.PrincipalConfig.class)
@DisplayName("UserManagementController — Slice Tests")
class UserManagementControllerTest {

    /**
     * Registra un HandlerMethodArgumentResolver que resuelve cualquier parámetro
     * anotado con @AuthenticationPrincipal como un AuthenticatedUser(id=1, ADMIN).
     * Esto evita depender de SecurityContextHolder (que no se propaga al hilo
     * del DispatcherServlet de MockMvc) ni de spring-security-test.
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class PrincipalConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(
                java.util.List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.hasParameterAnnotation(
                            org.springframework.security.core.annotation.AuthenticationPrincipal.class);
                }
                @Override
                public Object resolveArgument(MethodParameter parameter,
                                              ModelAndViewContainer mavContainer,
                                              NativeWebRequest webRequest,
                                              WebDataBinderFactory binderFactory) {
                    return new AuthenticatedUser(1L, "admin", "secret", UserRole.ADMIN, true,1L);
                }
            });
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserManagementService userManagementService;

    // --- Security beans required to avoid context failure ---
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ------------------------------------------------------------------ //
    //  Fixtures
    // ------------------------------------------------------------------ //

    private static final Long ADMIN_ID = 1L;

    private GuardianSummary guardianSummary;
    private UserDetailResponse userDetailResponse;
    private UserSummaryResponse userSummaryResponse;
    private CreateUserRequest validCreateRequest;
    private UpdateUserRoleRequest validRoleRequest;

    @BeforeEach
    void setUp() {
        guardianSummary = new GuardianSummary(10L, "Carlos López", "EMP-001", "TI");

        userDetailResponse = new UserDetailResponse(
                2L,
                "jdoe",
                "John Doe",
                "jdoe@unpa.mx",
                "EMP-042",
                UserRole.OPERADOR,
                true,
                LocalDateTime.of(2024, 5, 20, 8, 0),
                LocalDateTime.of(2024, 1, 10, 9, 0),
                LocalDateTime.of(2024, 6, 1, 11, 0),
                guardianSummary
        );

        userSummaryResponse = new UserSummaryResponse(
                2L,
                "jdoe",
                "John Doe",
                "jdoe@unpa.mx",
                "EMP-042",
                UserRole.OPERADOR,
                true,
                LocalDateTime.of(2024, 5, 20, 8, 0),
                LocalDateTime.of(2024, 1, 10, 9, 0),
                LocalDateTime.of(2024, 6, 1, 11, 0),
                guardianSummary
        );

        validCreateRequest = new CreateUserRequest(
                "jdoe",
                UserRole.OPERADOR,
                10L
        );

        validRoleRequest = new UpdateUserRoleRequest(UserRole.AUDITOR);
    }

    // ================================================================== //
    //  GET /v1/admin/users
    // ================================================================== //

    @Nested
    @DisplayName("GET /v1/admin/users — findAll")
    class FindAll {

        @Test
        @DisplayName("should_return200WithPage_when_noFiltersApplied")
        void should_return200WithPage_when_noFiltersApplied() throws Exception {
            Page<UserSummaryResponse> page = new PageImpl<>(List.of(userSummaryResponse), PageRequest.of(0, 15), 1);

            when(userManagementService.findAll(isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/v1/admin/users")
                            .param("page", "0")
                            .param("size", "15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(2)))
                    .andExpect(jsonPath("$.content[0].username", is("jdoe")))
                    .andExpect(jsonPath("$.content[0].role", is("OPERADOR")))
                    .andExpect(jsonPath("$.content[0].isActive", is(true)))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }

        @Test
        @DisplayName("should_return200WithFilteredPage_when_searchParamProvided")
        void should_return200WithFilteredPage_when_searchParamProvided() throws Exception {
            Page<UserSummaryResponse> page = new PageImpl<>(List.of(userSummaryResponse), PageRequest.of(0, 15), 1);

            when(userManagementService.findAll(eq("jdoe"), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/v1/admin/users")
                            .param("search", "jdoe"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].username", is("jdoe")));
        }

        @Test
        @DisplayName("should_return200WithFilteredPage_when_roleFilterProvided")
        void should_return200WithFilteredPage_when_roleFilterProvided() throws Exception {
            Page<UserSummaryResponse> page = new PageImpl<>(List.of(userSummaryResponse), PageRequest.of(0, 15), 1);

            when(userManagementService.findAll(isNull(), eq(UserRole.OPERADOR), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/v1/admin/users")
                            .param("role", "OPERADOR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].role", is("OPERADOR")));
        }

        @Test
        @DisplayName("should_return200WithFilteredPage_when_isActiveFilterProvided")
        void should_return200WithFilteredPage_when_isActiveFilterProvided() throws Exception {
            Page<UserSummaryResponse> page = new PageImpl<>(List.of(userSummaryResponse), PageRequest.of(0, 15), 1);

            when(userManagementService.findAll(isNull(), isNull(), eq(true), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/v1/admin/users")
                            .param("isActive", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].isActive", is(true)));
        }

        @Test
        @DisplayName("should_return200WithEmptyPage_when_noUsersMatch")
        void should_return200WithEmptyPage_when_noUsersMatch() throws Exception {
            Page<UserSummaryResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 15), 0);

            when(userManagementService.findAll(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            mockMvc.perform(get("/v1/admin/users")
                            .param("search", "inexistente"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }
    }

    // ================================================================== //
    //  GET /v1/admin/users/id
    // ================================================================== //

    @Nested
    @DisplayName("GET /v1/admin/users/{id} — findById")
    class FindById {

        @Test
        @DisplayName("should_return200WithUserDetail_when_idExists")
        void should_return200WithUserDetail_when_idExists() throws Exception {
            when(userManagementService.findById(2L)).thenReturn(userDetailResponse);

            mockMvc.perform(get("/v1/admin/users/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(2)))
                    .andExpect(jsonPath("$.data.username", is("jdoe")))
                    .andExpect(jsonPath("$.data.role", is("OPERADOR")))
                    .andExpect(jsonPath("$.data.isActive", is(true)))
                    .andExpect(jsonPath("$.data.guardian.id", is(10)))
                    .andExpect(jsonPath("$.data.guardian.fullName", is("Carlos López")))
                    .andExpect(jsonPath("$.data.guardian.employeeNumber", is("EMP-001")));
        }

        @Test
        @DisplayName("should_return404_when_userIdNotFound")
        void should_return404_when_userIdNotFound() throws Exception {
            when(userManagementService.findById(99L))
                    .thenThrow(new ResourceNotFoundException("Usuario no encontrado con id: 99"));

            mockMvc.perform(get("/v1/admin/users/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }

    // ================================================================== //
    //  POST /v1/admin/users
    // ================================================================== //

    @Nested
    @DisplayName("POST /v1/admin/users — create")
    class Create {

        @Test
        @DisplayName("should_return201WithCreatedUser_when_requestIsValid")
        void should_return201WithCreatedUser_when_requestIsValid() throws Exception {
            when(userManagementService.create(any(CreateUserRequest.class))).thenReturn(userDetailResponse);

            mockMvc.perform(post("/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(2)))
                    .andExpect(jsonPath("$.data.username", is("jdoe")))
                    .andExpect(jsonPath("$.data.role", is("OPERADOR")));
        }

        @Test
        @DisplayName("should_return400_when_usernameIsBlank")
        void should_return400_when_usernameIsBlank() throws Exception {
            CreateUserRequest badRequest = new CreateUserRequest("", UserRole.OPERADOR, 10L);

            mockMvc.perform(post("/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_usernameTooShort")
        void should_return400_when_usernameTooShort() throws Exception {
            CreateUserRequest badRequest = new CreateUserRequest("ab", UserRole.OPERADOR, 10L);

            mockMvc.perform(post("/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_roleIsNull")
        void should_return400_when_roleIsNull() throws Exception {
            CreateUserRequest badRequest = new CreateUserRequest("jdoe", null, 10L);

            mockMvc.perform(post("/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_guardianIdIsNull")
        void should_return400_when_guardianIdIsNull() throws Exception {
            CreateUserRequest badRequest = new CreateUserRequest("jdoe", UserRole.OPERADOR, null);

            mockMvc.perform(post("/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_requestBodyIsMissing")
        void should_return400_when_requestBodyIsMissing() throws Exception {
            mockMvc.perform(post("/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should_return409_when_usernameAlreadyExists")
        void should_return409_when_usernameAlreadyExists() throws Exception {
            when(userManagementService.create(any(CreateUserRequest.class)))
                    .thenThrow(new DuplicateResourceException("Ya existe un usuario con el username: jdoe"));

            mockMvc.perform(post("/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("jdoe")));
        }

        @Test
        @DisplayName("should_return404_when_guardianIdNotFound")
        void should_return404_when_guardianIdNotFound() throws Exception {
            when(userManagementService.create(any(CreateUserRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Resguardante no encontrado con id: 10"));

            mockMvc.perform(post("/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("10")));
        }
    }

    // ================================================================== //
    //  PATCH /v1/admin/users/{id}/role
    // ================================================================== //

    @Nested
    @DisplayName("PATCH /v1/admin/users/{id}/role — updateRole")
    class UpdateRole {

        /*
         * @AuthenticationPrincipal AuthenticatedUser currentUser se resuelve en null
         * cuando addFilters = false y no hay SecurityContext activo. Como el controlador
         * delega currentUser.id() directamente al servicio (sin lógica propia), se usa
         * El SecurityContext se puebla en @BeforeEach con ADMIN_ID=1L, por lo que
         * @AuthenticationPrincipal resuelve correctamente y podemos usar eq(ADMIN_ID).
         */

        @Test
        @DisplayName("should_return200WithUpdatedRole_when_requestIsValid")
        void should_return200WithUpdatedRole_when_requestIsValid() throws Exception {
            UserDetailResponse updatedResponse = new UserDetailResponse(
                    2L, "jdoe", "John Doe", "jdoe@unpa.mx", "EMP-042",
                    UserRole.AUDITOR, true, null,
                    LocalDateTime.of(2024, 1, 10, 9, 0),
                    LocalDateTime.of(2024, 6, 10, 12, 0),
                    guardianSummary
            );

            when(userManagementService.updateRole(eq(2L), any(UpdateUserRoleRequest.class), eq(ADMIN_ID)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(patch("/v1/admin/users/2/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRoleRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.role", is("AUDITOR")));
        }

        @Test
        @DisplayName("should_return400_when_roleIsNull")
        void should_return400_when_roleIsNull() throws Exception {
            UpdateUserRoleRequest badRequest = new UpdateUserRoleRequest(null);

            mockMvc.perform(patch("/v1/admin/users/2/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_requestBodyIsMissingOnRoleUpdate")
        void should_return400_when_requestBodyIsMissingOnRoleUpdate() throws Exception {
            mockMvc.perform(patch("/v1/admin/users/2/role")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should_return404_when_userToUpdateRoleNotFound")
        void should_return404_when_userToUpdateRoleNotFound() throws Exception {
            when(userManagementService.updateRole(eq(99L), any(UpdateUserRoleRequest.class), eq(ADMIN_ID)))
                    .thenThrow(new ResourceNotFoundException("Usuario no encontrado con id: 99"));

            mockMvc.perform(patch("/v1/admin/users/99/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRoleRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }

    // ================================================================== //
    //  PATCH /v1/admin/users/{id}/status
    // ================================================================== //

    @Nested
    @DisplayName("PATCH /v1/admin/users/{id}/status — toggleStatus")
    class ToggleStatus {

        @Test
        @DisplayName("should_return200WithDeactivatedUser_when_userWasActive")
        void should_return200WithDeactivatedUser_when_userWasActive() throws Exception {
            UserDetailResponse deactivated = new UserDetailResponse(
                    2L, "jdoe", "John Doe", "jdoe@unpa.mx", "EMP-042",
                    UserRole.OPERADOR, false, null,
                    LocalDateTime.of(2024, 1, 10, 9, 0),
                    LocalDateTime.of(2024, 6, 10, 14, 0),
                    guardianSummary
            );

            when(userManagementService.toggleStatus(2L, ADMIN_ID)).thenReturn(deactivated);

            mockMvc.perform(patch("/v1/admin/users/2/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.isActive", is(false)));
        }

        @Test
        @DisplayName("should_return200WithActivatedUser_when_userWasInactive")
        void should_return200WithActivatedUser_when_userWasInactive() throws Exception {
            when(userManagementService.toggleStatus(2L, ADMIN_ID)).thenReturn(userDetailResponse);

            mockMvc.perform(patch("/v1/admin/users/2/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.isActive", is(true)));
        }

        @Test
        @DisplayName("should_return404_when_userToToggleNotFound")
        void should_return404_when_userToToggleNotFound() throws Exception {
            when(userManagementService.toggleStatus(99L, ADMIN_ID))
                    .thenThrow(new ResourceNotFoundException("Usuario no encontrado con id: 99"));

            mockMvc.perform(patch("/v1/admin/users/99/status"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }

    // ================================================================== //
    //  POST /v1/admin/users/{id}/reset-password
    // ================================================================== //

    @Nested
    @DisplayName("POST /v1/admin/users/{id}/reset-password — resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("should_return200WithUserDetail_when_passwordResetSuccessfully")
        void should_return200WithUserDetail_when_passwordResetSuccessfully() throws Exception {
            when(userManagementService.resetPassword(2L)).thenReturn(userDetailResponse);

            mockMvc.perform(post("/v1/admin/users/2/reset-password"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(2)))
                    .andExpect(jsonPath("$.data.username", is("jdoe")));
        }

        @Test
        @DisplayName("should_return404_when_userToResetPasswordNotFound")
        void should_return404_when_userToResetPasswordNotFound() throws Exception {
            when(userManagementService.resetPassword(99L))
                    .thenThrow(new ResourceNotFoundException("Usuario no encontrado con id: 99"));

            mockMvc.perform(post("/v1/admin/users/99/reset-password"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }
}