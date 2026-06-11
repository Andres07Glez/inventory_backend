package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Guardian;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// SIN @TestPropertySource — ya está en application-test.properties
@org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
@org.springframework.test.context.ActiveProfiles("test")
class GuardianRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private GuardianRepository guardianRepository;

    // -------------------------------------------------------------------------
    // Helpers específicos de este test
    // -------------------------------------------------------------------------

    /** Guardian mínimo con los campos obligatorios. */
    private Guardian persistGuardian(String employeeNumber, String fullName) {
        return entityManager.persistAndFlush(
                buildGuardian(employeeNumber, fullName)
        );
    }

    /** Guardian con departamento explícito. */
    private Guardian persistGuardianWithDepartment(String employeeNumber,
                                                   String fullName,
                                                   String department) {
        Guardian g = buildGuardian(employeeNumber, fullName);
        g.setDepartment(department);
        return entityManager.persistAndFlush(g);
    }

    /** Guardian inactivo. */
    private Guardian persistInactiveGuardian(String employeeNumber, String fullName) {
        Guardian g = buildGuardian(employeeNumber, fullName);
        g.setIsActive(false);
        return entityManager.persistAndFlush(g);
    }

    // =========================================================================
    // findByEmployeeNumber
    // =========================================================================

    @Test
    void should_returnGuardian_when_employeeNumberExists() {
        persistGuardian("EMP-001", "Juan Pérez");

        Optional<Guardian> result = guardianRepository.findByEmployeeNumber("EMP-001");

        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Juan Pérez");
    }

    @Test
    void should_returnEmpty_when_employeeNumberDoesNotExist() {
        Optional<Guardian> result = guardianRepository.findByEmployeeNumber("NO-EXISTE");

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // existsByEmployeeNumber
    // =========================================================================

    @Test
    void should_returnTrue_when_employeeNumberAlreadyRegistered() {
        persistGuardian("EMP-002", "María López");

        boolean exists = guardianRepository.existsByEmployeeNumber("EMP-002");

        assertThat(exists).isTrue();
    }

    @Test
    void should_returnFalse_when_employeeNumberNotRegistered() {
        boolean exists = guardianRepository.existsByEmployeeNumber("EMP-GHOST");

        assertThat(exists).isFalse();
    }

    // =========================================================================
    // findByIsActiveTrue
    // =========================================================================

    @Test
    void should_returnOnlyActiveGuardians_when_mixedStatusExists() {
        persistGuardian("EMP-010", "Activo Uno");
        persistGuardian("EMP-011", "Activo Dos");
        persistInactiveGuardian("EMP-012", "Inactivo Uno");

        Pageable pageable = PageRequest.of(0, 10);
        Page<Guardian> page = guardianRepository.findByIsActiveTrue(pageable);

        assertThat(page.getContent())
                .extracting(Guardian::getEmployeeNumber)
                .containsExactlyInAnyOrder("EMP-010", "EMP-011")
                .doesNotContain("EMP-012");
    }

    @Test
    void should_returnEmptyPage_when_noActiveGuardiansExist() {
        persistInactiveGuardian("EMP-013", "Inactivo Solo");

        Page<Guardian> page = guardianRepository.findByIsActiveTrue(PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void should_respectPagination_when_activeGuardiansExceedPageSize() {
        for (int i = 1; i <= 5; i++) {
            persistGuardian("EMP-02" + i, "Guardian " + i);
        }

        Page<Guardian> firstPage  = guardianRepository.findByIsActiveTrue(PageRequest.of(0, 2));
        Page<Guardian> secondPage = guardianRepository.findByIsActiveTrue(PageRequest.of(1, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(secondPage.getContent()).hasSize(2);
    }

    // =========================================================================
    // searchActive — target principal (@Query JPQL)
    // =========================================================================

    // --- búsqueda por fullName ---

    @Test
    void should_findGuardian_when_queryMatchesFullName() {
        persistGuardian("EMP-100", "Carlos Mendoza Ruiz");

        Page<Guardian> result = guardianRepository.searchActive("Mendoza", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Guardian::getEmployeeNumber)
                .containsExactly("EMP-100");
    }

    @Test
    void should_findGuardian_when_queryMatchesFullNameCaseInsensitive() {
        persistGuardian("EMP-101", "Ana Gutierrez");

        Page<Guardian> result = guardianRepository.searchActive("GUTIERREZ", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmployeeNumber()).isEqualTo("EMP-101");
    }

    // --- búsqueda por employeeNumber ---

    @Test
    void should_findGuardian_when_queryMatchesEmployeeNumber() {
        persistGuardian("EMP-200", "Luis Torres");

        Page<Guardian> result = guardianRepository.searchActive("EMP-200", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Guardian::getFullName)
                .containsExactly("Luis Torres");
    }

    @Test
    void should_findGuardian_when_queryMatchesPartialEmployeeNumber() {
        persistGuardian("EMP-201", "Rosa Vega");
        persistGuardian("EMP-301", "Pedro Ríos");

        // "201" sólo coincide con EMP-201
        Page<Guardian> result = guardianRepository.searchActive("201", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Guardian::getEmployeeNumber)
                .containsExactly("EMP-201");
    }

    // --- búsqueda por department ---

    @Test
    void should_findGuardian_when_queryMatchesDepartment() {
        persistGuardianWithDepartment("EMP-300", "Sofía Ramírez", "Sistemas");
        persistGuardianWithDepartment("EMP-301", "Jorge Herrera", "Contabilidad");

        Page<Guardian> result = guardianRepository.searchActive("Sistemas", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Guardian::getEmployeeNumber)
                .containsExactly("EMP-300");
    }

    // --- edge case: department = null no debe romper el query ---

    @Test
    void should_notThrow_when_guardianHasNullDepartment() {
        // Guardian sin departamento: el LIKE sobre NULL devuelve false, no excepción
        persistGuardian("EMP-400", "Sin Departamento");

        Page<Guardian> result = guardianRepository.searchActive("Sistemas", PageRequest.of(0, 10));

        // No debe lanzar excepción; simplemente no debe estar en resultados
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void should_findGuardianByName_when_anotherGuardianHasNullDepartment() {
        persistGuardian("EMP-401", "Sin Depto");                          // department = null
        persistGuardianWithDepartment("EMP-402", "Con Depto", "Rectoría");

        Page<Guardian> result = guardianRepository.searchActive("rectoría", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Guardian::getEmployeeNumber)
                .containsExactly("EMP-402");
    }

    // --- solo activos ---

    @Test
    void should_excludeInactiveGuardians_when_searching() {
        persistGuardianWithDepartment("EMP-500", "Activo TI",   "TI");
        persistInactiveGuardian("EMP-501", "Inactivo TI");

        Page<Guardian> result = guardianRepository.searchActive("TI", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Guardian::getEmployeeNumber)
                .containsExactly("EMP-500")
                .doesNotContain("EMP-501");
    }

    // --- sin resultados ---

    @Test
    void should_returnEmptyPage_when_queryMatchesNothing() {
        persistGuardian("EMP-600", "Roberto Juárez");

        Page<Guardian> result = guardianRepository.searchActive("XYZ_NADA", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // --- múltiples coincidencias ---

    @Test
    void should_returnMultipleGuardians_when_queryMatchesSeveralFields() {
        persistGuardianWithDepartment("EMP-700", "Jorge López",     "TI");
        persistGuardianWithDepartment("EMP-701", "Laura Jiménez",   "TI");
        persistInactiveGuardian("EMP-702", "Inactivo TI");

        Page<Guardian> result = guardianRepository.searchActive("TI", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Guardian::getEmployeeNumber)
                .containsExactlyInAnyOrder("EMP-700", "EMP-701");
    }

    // --- paginación en searchActive ---

    @Test
    void should_paginateResults_when_searchReturnsMultiplePages() {
        for (int i = 1; i <= 4; i++) {
            persistGuardianWithDepartment("EMP-80" + i, "Docente " + i, "Académico");
        }

        Page<Guardian> firstPage = guardianRepository.searchActive("Académico", PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }
}