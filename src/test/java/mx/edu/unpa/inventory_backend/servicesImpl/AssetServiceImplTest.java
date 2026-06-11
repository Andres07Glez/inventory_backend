package mx.edu.unpa.inventory_backend.servicesImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import mx.edu.unpa.inventory_backend.components.InventoryNumberGenerator;
import mx.edu.unpa.inventory_backend.domains.*;
import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.request.UpdateConditionRequest;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.UpdateConditionResponse;
import mx.edu.unpa.inventory_backend.enums.Campus;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.InvalidAssetStateException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.mappers.AssetCommandMapper;
import mx.edu.unpa.inventory_backend.mappers.AssetMapper;
import mx.edu.unpa.inventory_backend.repositories.*;
import mx.edu.unpa.inventory_backend.services.impl.AssetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetServiceImpl")
class AssetServiceImplTest {

    // -----------------------------------------------------------------------
    // Collaborators
    // -----------------------------------------------------------------------
    @Mock private AssetRepository          assetRepository;
    @Mock private CategoryRepository       categoryRepository;
    @Mock private LocationRepository       locationRepository;
    @Mock private UserRepository           userRepository;
    @Mock private InvoiceRepository        invoiceRepository;
    @Mock private BrandRepository          brandRepository;
    @Mock private InventoryNumberGenerator inventoryNumberGenerator;
    @Mock private AssetMapper              assetMapper;
    @Mock private AssetCommandMapper       assetCommandMapper;
    @Mock private EntityManager            entityManager;
    @Mock private Query                    jpaQuery;

    @InjectMocks
    private AssetServiceImpl sut;

    // -----------------------------------------------------------------------
    // Fixtures de dominio — objetos Java puros, sin mocks ni stubs
    // -----------------------------------------------------------------------
    private User     creator;
    private Category category;
    private Location location;
    private Brand    brand;

    @BeforeEach
    void buildFixtures() {
        Guardian guardian = new Guardian();
        guardian.setId(1L);
        guardian.setFullName("Admin UNPA");

        creator = User.builder()
                .id(1L)
                .username("admin")
                .passwordHash("$2a$hash")
                .role(mx.edu.unpa.inventory_backend.enums.UserRole.ADMIN)
                .isActive(true)
                .guardian(guardian)
                .build();

        category = new Category();
        category.setId(1);
        category.setName("Computadoras");
        category.setIsActive(true);

        location = new Location();
        location.setId(1);
        location.setName("Laboratorio A");
        location.setBuilding("Edificio 3");
        location.setCampus(Campus.LOMA_BONITA);
        location.setIsActive(true);

        brand = new Brand();
        brand.setId(1);
        brand.setName("Dell");
        brand.setIsActive(true);
    }

    // =======================================================================
    // registerAsset
    // =======================================================================
    @Nested
    @DisplayName("registerAsset")
    class RegisterAsset {

        // -------------------------------------------------------------------
        // Builder de request — sin barcode ni serialNumber por defecto para
        //
        // no entren y no generen stubs huérfanos en tests que no los necesitan.
        // -------------------------------------------------------------------
        private AssetRequestDTO baseRequest() {
            AssetRequestDTO req = new AssetRequestDTO();
            req.setDescription("Laptop Dell Latitude");
            req.setCategoryId(1);
            req.setLocationId(1);
            req.setBrandId(1);
            req.setEntryDate(LocalDate.of(2025, 1, 10));
            req.setConditionStatus("GOOD");
            // barcode y serialNumber quedan null deliberadamente
            return req;
        }

        /** Asset que simula haber sido persistido (tiene ID asignado por la BD). */
        private Asset savedAsset(Long id, String inventoryNumber) {
            Asset a = new Asset();
            a.setId(id);
            a.setInventoryNumber(inventoryNumber);
            a.setDescription("Laptop Dell Latitude");
            a.setCategory(category);
            a.setLocation(location);
            a.setBrand(brand);
            a.setConditionStatus(ConditionStatus.GOOD);
            a.setLifecycleStatus(LifecycleStatus.REGISTERED);
            a.setEntryDate(LocalDate.of(2025, 1, 10));
            a.setCreatedBy(creator);
            a.setUpdatedBy(creator);
            a.setCreatedAt(LocalDateTime.of(2025, 1, 10, 9, 0));
            return a;
        }

        /**
         * Stubs que se consumen en el flujo completo hasta llegar a save().
         * Solo se llama desde tests que llegan hasta ese punto.
         * No incluye stubs condicionales (barcode, serialNumber, invoice)
         * porque dependen de lo que traiga cada request específico.
         */
        private void stubUntilSave(Asset returnedAsset) {
            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(category));
            when(locationRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(location));
            when(brandRepository.findById(1)).thenReturn(Optional.of(brand));
            when(assetRepository.save(any(Asset.class))).thenReturn(returnedAsset);
            doNothing().when(entityManager).flush();
        }

        /** Stubs del UPDATE JPQL que solo se ejecuta cuando inventoryNumber == "PENDING". */
        private void stubJpqlUpdate() {
            when(entityManager.createQuery(contains("UPDATE Asset"))).thenReturn(jpaQuery);
            when(jpaQuery.setParameter(eq("inv"), anyString())).thenReturn(jpaQuery);
            when(jpaQuery.setParameter(eq("id"), anyLong())).thenReturn(jpaQuery);
            when(jpaQuery.executeUpdate()).thenReturn(1);
        }

        // -------------------------------------------------------------------
        // Happy paths
        // -------------------------------------------------------------------

        @Test
        @DisplayName("should_autogenerateInventoryNumber_when_requestHasNoCustomInventoryNumber")
        void should_autogenerateInventoryNumber_when_requestHasNoCustomInventoryNumber() {
            // Arrange
            // inventoryNumber == null en baseRequest() → asset se guarda como "PENDING"
            Asset saved = savedAsset(1L, "PENDING");
            stubUntilSave(saved);
            stubJpqlUpdate();
            when(inventoryNumberGenerator.generate(1L)).thenReturn("INV-2025-00001");

            // Act
            sut.registerAsset(baseRequest(), 1L);

            // Assert
            verify(inventoryNumberGenerator, times(1)).generate(1L);
            verify(entityManager,            times(1)).createQuery(contains("UPDATE Asset"));
            verify(jpaQuery,                 times(1)).executeUpdate();
        }

        @Test
        @DisplayName("should_useCustomInventoryNumber_when_requestProvidesOne")
        void should_useCustomInventoryNumber_when_requestProvidesOne() {
            // Arrange
            AssetRequestDTO request = baseRequest();
            request.setInventoryNumber("INV-CUSTOM-001");

            // El asset ya sale con el número custom — no entra en la rama "PENDING"
            Asset saved = savedAsset(1L, "INV-CUSTOM-001");
            stubUntilSave(saved);
            // flush() ya está en stubUntilSave. No hay JPQL UPDATE.

            // Act
            sut.registerAsset(request, 1L);

            // Assert
            verify(inventoryNumberGenerator, never()).generate(anyLong());
            verify(entityManager,            never()).createQuery(anyString());
        }

        @Test
        @DisplayName("should_setConditionGood_when_requestHasNullConditionStatus")
        void should_setConditionGood_when_requestHasNullConditionStatus() {
            // Arrange
            AssetRequestDTO request = baseRequest();
            request.setConditionStatus(null);

            Asset saved = savedAsset(1L, "PENDING");
            stubUntilSave(saved);
            stubJpqlUpdate();
            when(inventoryNumberGenerator.generate(1L)).thenReturn("INV-2025-00001");

            // Act
            AssetResponseDTO result = sut.registerAsset(request, 1L);

            // Assert
            assertEquals("GOOD", result.getConditionStatus());
        }

        @Test
        @DisplayName("should_setConditionGood_when_requestHasBlankConditionStatus")
        void should_setConditionGood_when_requestHasBlankConditionStatus() {
            // Arrange
            AssetRequestDTO request = baseRequest();
            request.setConditionStatus("   ");

            Asset saved = savedAsset(1L, "PENDING");
            stubUntilSave(saved);
            stubJpqlUpdate();
            when(inventoryNumberGenerator.generate(1L)).thenReturn("INV-2025-00001");

            // Act & Assert
            assertDoesNotThrow(() -> sut.registerAsset(request, 1L));
        }

        @Test
        @DisplayName("should_acceptConditionStatusCaseInsensitive_when_valueIsLowerCase")
        void should_acceptConditionStatusCaseInsensitive_when_valueIsLowerCase() {
            // Arrange — el service hace toUpperCase() antes de valueOf()

            AssetRequestDTO request = baseRequest();
            request.setConditionStatus("regular");

            Asset saved = savedAsset(1L, "PENDING");
            stubUntilSave(saved);
            stubJpqlUpdate();
            when(inventoryNumberGenerator.generate(1L)).thenReturn("INV-2025-00001");

            // Act & Assert
            assertDoesNotThrow(() -> sut.registerAsset(request, 1L));
        }

        @Test
        @DisplayName("should_resolveInvoiceDateFromInvoice_when_requestHasNoInvoiceDate")
        void should_resolveInvoiceDateFromInvoice_when_requestHasNoInvoiceDate() {
            // Arrange
            Invoice invoice = new Invoice();
            invoice.setId(10L);
            invoice.setInvoiceDate(LocalDate.of(2024, 6, 15));

            AssetRequestDTO request = baseRequest();
            request.setInvoiceId(10L);
            request.setInvoiceDate(null);

            Asset saved = savedAsset(1L, "PENDING");
            saved.setInvoiceDate(LocalDate.of(2024, 6, 15));

            stubUntilSave(saved);
            // invoiceRepository se invoca porque invoiceId != null
            when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
            stubJpqlUpdate();
            when(inventoryNumberGenerator.generate(1L)).thenReturn("INV-2025-00001");

            // Act
            AssetResponseDTO result = sut.registerAsset(request, 1L);

            // Assert
            assertNotNull(result);
            verify(invoiceRepository, times(1)).findById(10L);
        }

        @Test
        @DisplayName("should_setNullLocation_when_requestHasNoLocationId")
        void should_setNullLocation_when_requestHasNoLocationId() {
            // Arrange
            AssetRequestDTO request = baseRequest();
            request.setLocationId(null); // resolveLocation() retorna null sin ir al repo

            Asset saved = savedAsset(1L, "PENDING");
            saved.setLocation(null);

            // locationRepository NO se llama — no lo stubeamos
            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(category));
            when(brandRepository.findById(1)).thenReturn(Optional.of(brand));
            when(assetRepository.save(any(Asset.class))).thenReturn(saved);
            doNothing().when(entityManager).flush();
            stubJpqlUpdate();
            when(inventoryNumberGenerator.generate(1L)).thenReturn("INV-2025-00001");

            // Act
            AssetResponseDTO result = sut.registerAsset(request, 1L);

            // Assert
            verify(locationRepository, never()).findByIdAndIsActiveTrue(anyInt());
            assertNull(result.getLocationName());
        }

        // -------------------------------------------------------------------
        // Excepciones — cada test stubea solo hasta el punto de corte del flujo
        //
        // Orden de ejecución en registerAsset():
        //   1. userRepository.findByIdAndIsActiveTrue        → lanza si vacío
        //   2. categoryRepository.findByIdAndIsActiveTrue    → lanza si vacío
        //   3. resolveLocation (locationRepository)          → lanza si id != null y vacío
        //   4. resolveInvoice  (invoiceRepository)           → lanza si id != null y vacío
        //   5. resolveBrand    (brandRepository)             → lanza si vacío o inactivo
        //   6. validateBarcode (existsByBarcode...)          → lanza si duplicado
        //   7. validateSerialNumber (existsBySerialNumber)   → lanza si duplicado
        //   8. resolveConditionStatus                        → lanza si valor inválido
        //   9. assetRepository.save()
        // -------------------------------------------------------------------

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_userIsInactiveOrNotFound")
        void should_throwResourceNotFoundException_when_userIsInactiveOrNotFound() {
            // Corte en paso 1 — nada más se ejecuta
            AssetRequestDTO request = baseRequest();
            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> sut.registerAsset(request, 1L));

            verify(categoryRepository, never()).findByIdAndIsActiveTrue(anyInt());
            verify(assetRepository,    never()).save(any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_categoryIsInactiveOrNotFound")
        void should_throwResourceNotFoundException_when_categoryIsInactiveOrNotFound() {
            // Corte en paso 2
            AssetRequestDTO request = baseRequest();
            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> sut.registerAsset(request, 1L));

            verify(locationRepository, never()).findByIdAndIsActiveTrue(anyInt());
            verify(assetRepository,    never()).save(any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_locationIdProvidedButNotFound")
        void should_throwResourceNotFoundException_when_locationIdProvidedButNotFound() {
            // Corte en paso 3 — location falla; brand e invoice no llegan a ejecutarse
            AssetRequestDTO request = baseRequest();
            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(category));
            when(locationRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> sut.registerAsset(request, 1L));

            verify(brandRepository,  never()).findById(anyInt());
            verify(assetRepository,  never()).save(any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_invoiceIdProvidedButNotFound")
        void should_throwResourceNotFoundException_when_invoiceIdProvidedButNotFound() {
            // Corte en paso 4 — invoice falla; brand aún no se ha ejecutado
            AssetRequestDTO request = baseRequest();
            request.setInvoiceId(99L);

            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(category));
            when(locationRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(location));
            when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
            // brandRepository NO se stubea — el flujo nunca llega al paso 5

            assertThrows(ResourceNotFoundException.class,
                    () -> sut.registerAsset(request, 1L));

            verify(brandRepository, never()).findById(anyInt());
            verify(assetRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_brandIsInactiveOrNotFound")
        void should_throwResourceNotFoundException_when_brandIsInactiveOrNotFound() {
            // Corte en paso 5 — brand inactivo; validaciones de unicidad no ejecutan
            Brand inactiveBrand = new Brand();
            inactiveBrand.setId(1);
            inactiveBrand.setIsActive(false);

            AssetRequestDTO request = baseRequest();
            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(category));
            when(locationRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(location));
            when(brandRepository.findById(1)).thenReturn(Optional.of(inactiveBrand));

            assertThrows(ResourceNotFoundException.class,
                    () -> sut.registerAsset(request, 1L));

            verify(assetRepository, never()).existsByBarcodeAndBarcodeIsNotNull(any());
            verify(assetRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_throwDuplicateResourceException_when_barcodeAlreadyExists")
        void should_throwDuplicateResourceException_when_barcodeAlreadyExists() {
            // Corte en paso 6 — barcode duplicado; serialNumber no se valida
            // barcode != null es prerequisito para que la rama entre
            AssetRequestDTO request = baseRequest();
            request.setBarcode("BAR-DUPLICADO");

            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(category));
            when(locationRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(location));
            when(brandRepository.findById(1)).thenReturn(Optional.of(brand));
            when(assetRepository.existsByBarcodeAndBarcodeIsNotNull("BAR-DUPLICADO"))
                    .thenReturn(true);

            assertThrows(DuplicateResourceException.class,
                    () -> sut.registerAsset(request, 1L));

            verify(assetRepository, never()).existsBySerialNumber(any());
            verify(assetRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_throwDuplicateResourceException_when_serialNumberAlreadyExists")
        void should_throwDuplicateResourceException_when_serialNumberAlreadyExists() {
            // Corte en paso 7 — barcode es null (no entra al if), serialNumber duplicado
            AssetRequestDTO request = baseRequest();
            request.setSerialNumber("SN-DUPLICADO");
            // barcode sigue null → existsByBarcodeAndBarcodeIsNotNull NO se invoca

            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(category));
            when(locationRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(location));
            when(brandRepository.findById(1)).thenReturn(Optional.of(brand));
            when(assetRepository.existsBySerialNumber("SN-DUPLICADO")).thenReturn(true);

            assertThrows(DuplicateResourceException.class,
                    () -> sut.registerAsset(request, 1L));

            verify(assetRepository, never()).existsByBarcodeAndBarcodeIsNotNull(any());
            verify(assetRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_throwIllegalArgumentException_when_conditionStatusValueIsUnknown")
        void should_throwIllegalArgumentException_when_conditionStatusValueIsUnknown() {
            // Corte en paso 8 — condición inválida.
            // barcode y serialNumber son null → sus ramas if no entran → no hay stubs de unicidad.
            AssetRequestDTO request = baseRequest();
            request.setConditionStatus("EXCELENTE");

            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(categoryRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(category));
            when(locationRepository.findByIdAndIsActiveTrue(1)).thenReturn(Optional.of(location));
            when(brandRepository.findById(1)).thenReturn(Optional.of(brand));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> sut.registerAsset(request, 1L));

            assertTrue(ex.getMessage().contains("Condición inválida"));
            verify(assetRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_notCallBarcodeValidation_when_barcodeIsNull")
        void should_notCallBarcodeValidation_when_barcodeIsNull() {
            // baseRequest() ya tiene barcode == null — la rama if no entra
            Asset saved = savedAsset(1L, "PENDING");
            stubUntilSave(saved);
            stubJpqlUpdate();
            when(inventoryNumberGenerator.generate(1L)).thenReturn("INV-2025-00001");

            sut.registerAsset(baseRequest(), 1L);

            verify(assetRepository, never()).existsByBarcodeAndBarcodeIsNotNull(any());
        }
    }

    // =======================================================================
    // updateCondition
    // =======================================================================
    @Nested
    @DisplayName("updateCondition")
    class UpdateCondition {

        private Asset modifiableAsset(Long id, ConditionStatus condition) {
            Asset a = new Asset();
            a.setId(id);
            a.setInventoryNumber("INV-2025-00001");
            a.setDescription("Laptop Dell");
            a.setCategory(category);
            a.setConditionStatus(condition);
            a.setLifecycleStatus(LifecycleStatus.AVAILABLE);
            a.setCreatedBy(creator);
            a.setUpdatedBy(creator);
            a.setUpdatedAt(LocalDateTime.of(2025, 6, 1, 10, 0));
            return a;
        }

        @Test
        @DisplayName("should_returnUpdateConditionResponse_when_assetIsModifiableAndUserExists")
        void should_returnUpdateConditionResponse_when_assetIsModifiableAndUserExists() {
            // Arrange
            Asset asset = modifiableAsset(1L, ConditionStatus.GOOD);
            UpdateConditionRequest request = new UpdateConditionRequest(ConditionStatus.REGULAR);

            UpdateConditionResponse expectedResponse = new UpdateConditionResponse(
                    1L, "INV-2025-00001",
                    ConditionStatus.GOOD, ConditionStatus.REGULAR,
                    LocalDateTime.of(2025, 6, 1, 10, 0));

            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(assetRepository.save(asset)).thenReturn(asset);
            when(assetCommandMapper.toUpdateConditionResponse(asset, ConditionStatus.GOOD))
                    .thenReturn(expectedResponse);

            // Act
            UpdateConditionResponse result = sut.updateCondition(1L, request, 1L);

            // Assert
            assertNotNull(result);
            assertEquals(ConditionStatus.GOOD,    result.previousCondition());
            assertEquals(ConditionStatus.REGULAR, result.newCondition());
            assertEquals(1L,                      result.assetId());
            verify(assetRepository, times(1)).save(asset);
        }

        @Test
        @DisplayName("should_capturePreviousCondition_before_applyingChange")
        void should_capturePreviousCondition_before_applyingChange() {
            // Arrange — verifica que previousCondition se captura ANTES de mutar el asset
            Asset asset = modifiableAsset(1L, ConditionStatus.REGULAR);
            UpdateConditionRequest request = new UpdateConditionRequest(ConditionStatus.BAD);

            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(assetRepository.save(asset)).thenReturn(asset);
            when(assetCommandMapper.toUpdateConditionResponse(any(), eq(ConditionStatus.REGULAR)))
                    .thenReturn(new UpdateConditionResponse(
                            1L, "INV-2025-00001",
                            ConditionStatus.REGULAR, ConditionStatus.BAD,
                            LocalDateTime.now()));

            // Act
            sut.updateCondition(1L, request, 1L);

            // Assert — si se pasara BAD aquí el test fallaría, garantizando el orden de captura
            verify(assetCommandMapper, times(1))
                    .toUpdateConditionResponse(any(), eq(ConditionStatus.REGULAR));
        }

        @Test
        @DisplayName("should_allowModification_when_lifecycleStatusIsNotDecommissioned")
        void should_allowModification_when_lifecycleStatusIsNotDecommissioned() {
            // Arrange — IN_MAINTENANCE es un estado válido para modificar
            Asset asset = modifiableAsset(1L, ConditionStatus.GOOD);
            asset.setLifecycleStatus(LifecycleStatus.IN_MAINTENANCE);
            UpdateConditionRequest request = new UpdateConditionRequest(ConditionStatus.REGULAR);

            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(assetRepository.save(asset)).thenReturn(asset);
            when(assetCommandMapper.toUpdateConditionResponse(any(), any()))
                    .thenReturn(new UpdateConditionResponse(
                            1L, "INV-2025-00001",
                            ConditionStatus.GOOD, ConditionStatus.REGULAR,
                            LocalDateTime.now()));

            // Act & Assert
            assertDoesNotThrow(() -> sut.updateCondition(1L, request, 1L));
        }

        @Test
        @DisplayName("should_throwInvalidAssetStateException_when_assetIsDecommissioned")
        void should_throwInvalidAssetStateException_when_assetIsDecommissioned() {
            // Arrange — DECOMMISSIONED es el único estado que bloquea la modificación
            Asset asset = modifiableAsset(1L, ConditionStatus.BAD);
            asset.setLifecycleStatus(LifecycleStatus.DECOMMISSIONED);

            // EXTRAER LA VARIABLE AQUÍ
            UpdateConditionRequest request = new UpdateConditionRequest(ConditionStatus.GOOD);

            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

            // Act & Assert
            assertThrows(InvalidAssetStateException.class,
                    () -> sut.updateCondition(1L, request, 1L));

            verify(assetRepository,    never()).save(any());
            verify(assetCommandMapper, never()).toUpdateConditionResponse(any(), any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_assetDoesNotExist")
        void should_throwResourceNotFoundException_when_assetDoesNotExist() {
            // Corte en el primer findById — userRepository no se consulta
            UpdateConditionRequest request = new UpdateConditionRequest(ConditionStatus.BAD);
            when(assetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> sut.updateCondition(99L, request, 1L));

            verify(userRepository,  never()).findById(anyLong());
            verify(assetRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_userDoesNotExist")
        void should_throwResourceNotFoundException_when_userDoesNotExist() {
            // Corte en el segundo findById — asset existe, user no
            Asset asset = modifiableAsset(1L, ConditionStatus.GOOD);

            UpdateConditionRequest request = new UpdateConditionRequest(ConditionStatus.BAD);

            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> sut.updateCondition(1L, request, 99L));

            verify(assetRepository, never()).save(any());
        }
    }

    // =======================================================================
    // getAllAssets (pass-through — solo verifica la delegación al repositorio)
    // =======================================================================
    @Nested
    @DisplayName("getAllAssets")
    class GetAllAssets {

        @Test
        @DisplayName("should_delegateToRepository_with_allFilters")
        void should_delegateToRepository_with_allFilters() {
            // Arrange
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 10);
            org.springframework.data.domain.Page<Asset> emptyPage =
                    org.springframework.data.domain.Page.empty(pageable);

            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end   = LocalDate.of(2025, 12, 31);

            when(assetRepository.findFiltered(
                    ConditionStatus.GOOD, LifecycleStatus.AVAILABLE, start, end, pageable))
                    .thenReturn(emptyPage);

            // Act
            sut.getAllAssets(ConditionStatus.GOOD, LifecycleStatus.AVAILABLE, start, end, pageable);

            // Assert
            verify(assetRepository, times(1))
                    .findFiltered(ConditionStatus.GOOD, LifecycleStatus.AVAILABLE, start, end, pageable);
        }

        @Test
        @DisplayName("should_delegateToRepository_when_allFiltersAreNull")
        void should_delegateToRepository_when_allFiltersAreNull() {
            // Arrange
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);

            when(assetRepository.findFiltered(null, null, null, null, pageable))
                    .thenReturn(org.springframework.data.domain.Page.empty(pageable));

            // Act
            sut.getAllAssets(null, null, null, null, pageable);

            // Assert
            verify(assetRepository, times(1))
                    .findFiltered(null, null, null, null, pageable);
        }
    }
}