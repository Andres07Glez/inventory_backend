package mx.edu.unpa.inventory_backend.services.impl;

import mx.edu.unpa.inventory_backend.domains.Supplier;
import mx.edu.unpa.inventory_backend.dtos.supplier.request.SupplierRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.supplier.response.SupplierResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.mappers.SupplierMapper;
import mx.edu.unpa.inventory_backend.repositories.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    @Mock private SupplierRepository supplierRepository;
    // SupplierMapper usa componentModel="spring" → bean gestionado por Spring.
    // Se mockea con @Mock para no necesitar levantar contexto ni resolver la
    // implementación generada por MapStruct en tiempo de compilación (Problema 1).
    @Mock private SupplierMapper supplierMapper;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    // ─────────────────────────────────────────────
    //  Factories de stubs
    // ─────────────────────────────────────────────

    private Supplier stubSupplier(Long id, String name, String rfc) {
        Supplier s = new Supplier();
        s.setId(id);
        s.setName(name);
        s.setRfc(rfc);
        s.setContactName("Contacto " + name);
        s.setIsActive(true);
        s.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        s.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        return s;
    }

    private SupplierRequestDTO buildRequest(String name, String rfc) {
        return new SupplierRequestDTO(name, "Contacto " + name, null, null, null, null, rfc);
    }

    private SupplierResponseDTO stubResponse(Long id, String name, String rfc) {
        return new SupplierResponseDTO(
                id, name, rfc, "Contacto " + name,
                null, null, null, null,
                true,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }

    // ─────────────────────────────────────────────
    //  create — happy paths
    // ─────────────────────────────────────────────

    @Test
    void should_returnResponseDTO_when_createWithValidNameAndRfc() {
        // Arrange
        SupplierRequestDTO request  = buildRequest("Proveedor Alpha", "AAA010101AAA");
        Supplier           entity   = stubSupplier(null, "Proveedor Alpha", "AAA010101AAA");
        Supplier           saved    = stubSupplier(1L,   "Proveedor Alpha", "AAA010101AAA");
        SupplierResponseDTO expected = stubResponse(1L,  "Proveedor Alpha", "AAA010101AAA");

        when(supplierRepository.existsByName("Proveedor Alpha")).thenReturn(false);
        when(supplierRepository.existsByRfc("AAA010101AAA")).thenReturn(false);
        when(supplierMapper.toEntity(request)).thenReturn(entity);
        when(supplierRepository.save(entity)).thenReturn(saved);
        when(supplierMapper.toDto(saved)).thenReturn(expected);

        // Act
        SupplierResponseDTO result = supplierService.create(request);

        // Assert
        assertNotNull(result);
        assertEquals(1L,               result.id());
        assertEquals("Proveedor Alpha", result.name());
        assertEquals("AAA010101AAA",   result.rfc());
        verify(supplierRepository).save(entity);
    }

    @Test
    void should_setIsActiveTrueOnEntity_when_create() {
        // Arrange — verificamos que el servicio fuerza isActive=true
        // independientemente de lo que devuelva el mapper
        SupplierRequestDTO request = buildRequest("Proveedor Beta", null);
        Supplier entity = stubSupplier(null, "Proveedor Beta", null);
        entity.setIsActive(false); // simulamos mapper que no setea el flag

        when(supplierRepository.existsByName("Proveedor Beta")).thenReturn(false);
        when(supplierMapper.toEntity(request)).thenReturn(entity);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(entity);
        when(supplierMapper.toDto(any())).thenReturn(stubResponse(1L, "Proveedor Beta", null));

        // Act
        supplierService.create(request);

        // Assert
        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(captor.capture());
        assertTrue(captor.getValue().getIsActive());
    }

    @Test
    void should_skipRfcCheck_when_createWithNullRfc() {
        // Arrange
        SupplierRequestDTO request = buildRequest("Proveedor Sin RFC", null);
        Supplier entity = stubSupplier(null, "Proveedor Sin RFC", null);
        Supplier saved  = stubSupplier(2L,   "Proveedor Sin RFC", null);

        when(supplierRepository.existsByName("Proveedor Sin RFC")).thenReturn(false);
        when(supplierMapper.toEntity(request)).thenReturn(entity);
        when(supplierRepository.save(entity)).thenReturn(saved);
        when(supplierMapper.toDto(saved)).thenReturn(stubResponse(2L, "Proveedor Sin RFC", null));

        // Act
        supplierService.create(request);

        // Assert — con RFC null, nunca se consulta existsByRfc
        verify(supplierRepository, never()).existsByRfc(any());
    }

    @Test
    void should_skipRfcCheck_when_createWithBlankRfc() {
        // Arrange — RFC en blanco es tratado igual que null
        SupplierRequestDTO request = new SupplierRequestDTO(
                "Proveedor RFC Blanco", "Contacto", null, null, null, null, "   ");
        Supplier entity = stubSupplier(null, "Proveedor RFC Blanco", null);
        Supplier saved  = stubSupplier(3L,   "Proveedor RFC Blanco", null);

        when(supplierRepository.existsByName("Proveedor RFC Blanco")).thenReturn(false);
        when(supplierMapper.toEntity(request)).thenReturn(entity);
        when(supplierRepository.save(entity)).thenReturn(saved);
        when(supplierMapper.toDto(saved)).thenReturn(stubResponse(3L, "Proveedor RFC Blanco", null));

        // Act
        supplierService.create(request);

        // Assert
        verify(supplierRepository, never()).existsByRfc(any());
    }

    // ─────────────────────────────────────────────
    //  create — errores de duplicado
    // ─────────────────────────────────────────────

    @Test
    void should_throwDuplicateResourceException_when_createWithExistingName() {
        // Arrange
        SupplierRequestDTO request = buildRequest("Nombre Duplicado", null);
        when(supplierRepository.existsByName("Nombre Duplicado")).thenReturn(true);

        // Act & Assert
        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> supplierService.create(request)
        );
        assertTrue(ex.getMessage().contains("Nombre Duplicado"));
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void should_throwDuplicateResourceException_when_createWithExistingRfc() {
        // Arrange
        SupplierRequestDTO request = buildRequest("Proveedor Nuevo", "BBB020202BBB");
        when(supplierRepository.existsByName("Proveedor Nuevo")).thenReturn(false);
        when(supplierRepository.existsByRfc("BBB020202BBB")).thenReturn(true);

        // Act & Assert
        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> supplierService.create(request)
        );
        assertTrue(ex.getMessage().contains("BBB020202BBB"));
        verify(supplierRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────
    //  findAllActive
    // ─────────────────────────────────────────────

    @Test
    void should_returnPageOfActiveDTOs_when_findAllActive() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Supplier s1 = stubSupplier(1L, "Activo Uno", null);
        Supplier s2 = stubSupplier(2L, "Activo Dos", null);
        Page<Supplier> supplierPage = new PageImpl<>(List.of(s1, s2), pageable, 2);

        SupplierResponseDTO dto1 = stubResponse(1L, "Activo Uno", null);
        SupplierResponseDTO dto2 = stubResponse(2L, "Activo Dos", null);

        when(supplierRepository.findByIsActiveTrue(pageable)).thenReturn(supplierPage);
        when(supplierMapper.toDto(s1)).thenReturn(dto1);
        when(supplierMapper.toDto(s2)).thenReturn(dto2);

        // Act
        Page<SupplierResponseDTO> result = supplierService.findAllActive(pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        assertEquals("Activo Uno", result.getContent().get(0).name());
        assertEquals("Activo Dos", result.getContent().get(1).name());
        verify(supplierRepository).findByIsActiveTrue(pageable);
    }

    @Test
    void should_returnEmptyPage_when_findAllActiveAndNoSuppliersExist() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(supplierRepository.findByIsActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        Page<SupplierResponseDTO> result = supplierService.findAllActive(pageable);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ─────────────────────────────────────────────
    //  search
    // ─────────────────────────────────────────────

    @Test
    void should_returnMatchingDTOs_when_searchWithQuery() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Supplier s = stubSupplier(1L, "Electronica SA", null);
        SupplierResponseDTO dto = stubResponse(1L, "Electronica SA", null);
        Page<Supplier> page = new PageImpl<>(List.of(s), pageable, 1);

        when(supplierRepository.searchActive("Electronica", pageable)).thenReturn(page);
        when(supplierMapper.toDto(s)).thenReturn(dto);

        // Act
        Page<SupplierResponseDTO> result = supplierService.search("Electronica", pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Electronica SA", result.getContent().get(0).name());
        verify(supplierRepository).searchActive("Electronica", pageable);
    }

    @Test
    void should_returnEmptyPage_when_searchWithNoMatchingQuery() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(supplierRepository.searchActive("xyz_inexistente", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        Page<SupplierResponseDTO> result = supplierService.search("xyz_inexistente", pageable);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ─────────────────────────────────────────────
    //  findById
    // ─────────────────────────────────────────────

    @Test
    void should_returnResponseDTO_when_findByIdAndSupplierExists() {
        // Arrange
        Supplier supplier = stubSupplier(1L, "Proveedor Uno", "CCC030303CCC");
        SupplierResponseDTO expected = stubResponse(1L, "Proveedor Uno", "CCC030303CCC");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierMapper.toDto(supplier)).thenReturn(expected);

        // Act
        SupplierResponseDTO result = supplierService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L,             result.id());
        assertEquals("Proveedor Uno", result.name());
    }

    @Test
    void should_throwResourceNotFoundException_when_findByIdAndSupplierDoesNotExist() {
        // Arrange
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.findById(99L)
        );
        assertTrue(ex.getMessage().contains("99"));
    }

    // ─────────────────────────────────────────────
    //  update — happy paths
    // ─────────────────────────────────────────────

    @Test
    void should_returnUpdatedResponseDTO_when_updateWithSameNameAndSameRfc() {
        // Arrange — nombre y RFC no cambian → no se consultan duplicados
        Supplier existing = stubSupplier(1L, "Proveedor Uno", "CCC030303CCC");
        SupplierRequestDTO request = buildRequest("Proveedor Uno", "CCC030303CCC");
        SupplierResponseDTO expected = stubResponse(1L, "Proveedor Uno", "CCC030303CCC");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(existing)).thenReturn(existing);
        when(supplierMapper.toDto(existing)).thenReturn(expected);

        // Act
        SupplierResponseDTO result = supplierService.update(1L, request);

        // Assert
        assertNotNull(result);
        verify(supplierRepository, never()).existsByNameAndIdNot(any(), any());
        verify(supplierRepository, never()).existsByRfcAndIdNot(any(), any());
        verify(supplierRepository).save(existing);
    }

    @Test
    void should_updateSuccessfully_when_nameChangesToUniqueValue() {
        // Arrange
        Supplier existing = stubSupplier(1L, "Nombre Viejo", "DDD040404DDD");
        SupplierRequestDTO request = buildRequest("Nombre Nuevo", "DDD040404DDD");
        SupplierResponseDTO expected = stubResponse(1L, "Nombre Nuevo", "DDD040404DDD");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByNameAndIdNot("Nombre Nuevo", 1L)).thenReturn(false);
        when(supplierRepository.save(existing)).thenReturn(existing);
        when(supplierMapper.toDto(existing)).thenReturn(expected);

        // Act
        SupplierResponseDTO result = supplierService.update(1L, request);

        // Assert
        assertNotNull(result);
        verify(supplierRepository).existsByNameAndIdNot("Nombre Nuevo", 1L);
    }

    @Test
    void should_updateSuccessfully_when_rfcChangesToUniqueValue() {
        // Arrange
        Supplier existing = stubSupplier(1L, "Proveedor Uno", "EEE050505EEE");
        SupplierRequestDTO request = buildRequest("Proveedor Uno", "FFF060606FFF");
        SupplierResponseDTO expected = stubResponse(1L, "Proveedor Uno", "FFF060606FFF");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByRfcAndIdNot("FFF060606FFF", 1L)).thenReturn(false);
        when(supplierRepository.save(existing)).thenReturn(existing);
        when(supplierMapper.toDto(existing)).thenReturn(expected);

        // Act
        SupplierResponseDTO result = supplierService.update(1L, request);

        // Assert
        assertNotNull(result);
        verify(supplierRepository).existsByRfcAndIdNot("FFF060606FFF", 1L);
    }

    @Test
    void should_checkRfcDuplicate_when_updateAndSupplierHadNullRfc() {
        // Edge case: el proveedor no tenía RFC (null en BD) y el request envía uno.
        // En este caso rfcCambiado = true porque supplier.getRfc() == null.
        Supplier existing = stubSupplier(1L, "Proveedor Sin RFC", null);
        SupplierRequestDTO request = buildRequest("Proveedor Sin RFC", "GGG070707GGG");
        SupplierResponseDTO expected = stubResponse(1L, "Proveedor Sin RFC", "GGG070707GGG");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByRfcAndIdNot("GGG070707GGG", 1L)).thenReturn(false);
        when(supplierRepository.save(existing)).thenReturn(existing);
        when(supplierMapper.toDto(existing)).thenReturn(expected);

        // Act
        supplierService.update(1L, request);

        // Assert — debe consultar existsByRfcAndIdNot porque el RFC cambió de null a valor
        verify(supplierRepository).existsByRfcAndIdNot("GGG070707GGG", 1L);
    }

    @Test
    void should_skipRfcDuplicateCheck_when_updateWithNullRfc() {
        // Arrange — request sin RFC: no se debe verificar duplicado de RFC
        Supplier existing = stubSupplier(1L, "Proveedor Uno", "HHH080808HHH");
        SupplierRequestDTO request = buildRequest("Proveedor Uno", null);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(existing)).thenReturn(existing);
        when(supplierMapper.toDto(existing)).thenReturn(stubResponse(1L, "Proveedor Uno", null));

        // Act
        supplierService.update(1L, request);

        // Assert
        verify(supplierRepository, never()).existsByRfcAndIdNot(any(), any());
    }

    @Test
    void should_skipNameDuplicateCheck_when_updateWithSameNameDifferentCase() {
        // Edge case: el nombre enviado difiere solo en mayúsculas/minúsculas
        // del nombre existente — equalsIgnoreCase = true → no se verifica duplicado
        Supplier existing = stubSupplier(1L, "Proveedor Uno", null);
        SupplierRequestDTO request = buildRequest("PROVEEDOR UNO", null);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(existing)).thenReturn(existing);
        when(supplierMapper.toDto(existing)).thenReturn(stubResponse(1L, "PROVEEDOR UNO", null));

        // Act
        supplierService.update(1L, request);

        // Assert — misma cadena en distinto case → no hay cambio de nombre real
        verify(supplierRepository, never()).existsByNameAndIdNot(any(), any());
    }

    @Test
    void should_callUpdateEntityFromDto_when_updateSucceeds() {
        // Arrange — verificamos que el mapper de actualización es invocado con los parámetros correctos
        Supplier existing = stubSupplier(1L, "Proveedor Uno", null);
        SupplierRequestDTO request = buildRequest("Proveedor Uno Editado", null);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByNameAndIdNot("Proveedor Uno Editado", 1L)).thenReturn(false);
        when(supplierRepository.save(existing)).thenReturn(existing);
        when(supplierMapper.toDto(existing)).thenReturn(stubResponse(1L, "Proveedor Uno Editado", null));

        // Act
        supplierService.update(1L, request);

        // Assert
        verify(supplierMapper).updateEntityFromDto(request, existing);
    }

    // ─────────────────────────────────────────────
    //  update — errores de duplicado
    // ─────────────────────────────────────────────

    @Test
    void should_throwDuplicateResourceException_when_updateWithExistingNameOfOtherSupplier() {
        // Arrange
        Supplier existing = stubSupplier(1L, "Nombre Viejo", null);
        SupplierRequestDTO request = buildRequest("Nombre Tomado", null);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByNameAndIdNot("Nombre Tomado", 1L)).thenReturn(true);

        // Act & Assert
        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> supplierService.update(1L, request)
        );
        assertTrue(ex.getMessage().contains("Nombre Tomado"));
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void should_throwDuplicateResourceException_when_updateWithExistingRfcOfOtherSupplier() {
        // Arrange
        Supplier existing = stubSupplier(1L, "Proveedor Uno", "III090909III");
        SupplierRequestDTO request = buildRequest("Proveedor Uno", "JJJ101010JJJ");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByRfcAndIdNot("JJJ101010JJJ", 1L)).thenReturn(true);

        // Act & Assert
        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> supplierService.update(1L, request)
        );
        assertTrue(ex.getMessage().contains("JJJ101010JJJ"));
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void should_throwResourceNotFoundException_when_updateAndSupplierDoesNotExist() {
        // Arrange
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());
        SupplierRequestDTO request = buildRequest("No Importa", null);

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.update(99L, request)
        );
        assertTrue(ex.getMessage().contains("99"));
        verify(supplierRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────
    //  deactivate
    // ─────────────────────────────────────────────

    @Test
    void should_setIsActiveFalseAndSave_when_deactivateExistingSupplier() {
        // Arrange
        Supplier existing = stubSupplier(1L, "Proveedor Activo", null);
        assertTrue(existing.getIsActive()); // pre-condición explícita

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(existing);

        // Act
        supplierService.deactivate(1L);

        // Assert — capturamos el objeto para verificar que isActive fue puesto en false
        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(captor.capture());
        assertFalse(captor.getValue().getIsActive());
    }

    @Test
    void should_throwResourceNotFoundException_when_deactivateAndSupplierDoesNotExist() {
        // Arrange
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.deactivate(99L)
        );
        assertTrue(ex.getMessage().contains("99"));
        verify(supplierRepository, never()).save(any());
    }
}