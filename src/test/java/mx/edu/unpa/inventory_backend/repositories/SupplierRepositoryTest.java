package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

// SIN @TestPropertySource — ya está en application-test.properties
class SupplierRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private SupplierRepository supplierRepository;

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    /**
     * Crea y persiste un Supplier activo con los campos mínimos obligatorios.
     * FIX: isActive se setea EXPLÍCITAMENTE (no confiar en el inicializador de campo
     * de Java, Hibernate 6 + H2 puede omitirlo del INSERT si no está "dirty").
     */
    private Supplier persistSupplier(String name, String rfc) {
        Supplier s = new Supplier();
        s.setName(name);
        s.setRfc(rfc);
        s.setContactName("Contacto " + name);
        s.setIsActive(true); // FIX: explícito, no depender del default Java
        return entityManager.persistAndFlush(s);
    }

    /** Persiste un Supplier con isActive = false. */
    private Supplier persistInactiveSupplier(String name, String rfc) {
        Supplier s = new Supplier();
        s.setName(name);
        s.setRfc(rfc);
        s.setContactName("Contacto " + name);
        s.setIsActive(false);
        return entityManager.persistAndFlush(s);
    }

    // ─────────────────────────────────────────────
    //  existsByName
    // ─────────────────────────────────────────────

    @Test
    void should_returnTrue_when_nameAlreadyExists() {
        // Arrange
        persistSupplier("Proveedor Alpha", "AAA010101AAA");
        // FIX: limpiar caché de primer nivel para que existsBy* vaya a BD
        entityManager.clear();

        // Act
        boolean result = supplierRepository.existsByName("Proveedor Alpha");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void should_returnFalse_when_nameDoesNotExist() {
        // Act
        boolean result = supplierRepository.existsByName("Proveedor Inexistente");

        // Assert
        assertThat(result).isFalse();
    }

    // ─────────────────────────────────────────────
    //  existsByNameAndIdNot  (validación de unicidad en update)
    // ─────────────────────────────────────────────

    @Test
    void should_returnTrue_when_nameExistsForDifferentSupplier() {
        // Arrange
        persistSupplier("Proveedor Beta", "BBB020202BBB");
        Supplier other = persistSupplier("Proveedor Gamma", "CCC030303CCC");
        entityManager.clear(); // FIX: flush ya ocurrió en persistAndFlush; clear fuerza consulta a BD

        // Act
        boolean result = supplierRepository.existsByNameAndIdNot("Proveedor Beta", other.getId());

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void should_returnFalse_when_nameExistsButBelongsToSameSupplier() {
        // Arrange
        Supplier s = persistSupplier("Proveedor Delta", "DDD040404DDD");
        entityManager.clear();

        // Act
        boolean result = supplierRepository.existsByNameAndIdNot("Proveedor Delta", s.getId());

        // Assert
        assertThat(result).isFalse();
    }

    // ─────────────────────────────────────────────
    //  existsByRfc
    // ─────────────────────────────────────────────

    @Test
    void should_returnTrue_when_rfcAlreadyExists() {
        // Arrange
        persistSupplier("Proveedor Epsilon", "EEE050505EEE");
        entityManager.clear(); // FIX: invalidar caché antes de existsBy*

        // Act
        boolean result = supplierRepository.existsByRfc("EEE050505EEE");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void should_returnFalse_when_rfcDoesNotExist() {
        // Act
        boolean result = supplierRepository.existsByRfc("ZZZ999999ZZZ");

        // Assert
        assertThat(result).isFalse();
    }

    // ─────────────────────────────────────────────
    //  existsByRfcAndIdNot  (validación de unicidad en update)
    // ─────────────────────────────────────────────

    @Test
    void should_returnTrue_when_rfcExistsForDifferentSupplier() {
        // Arrange
        persistSupplier("Proveedor Zeta", "FFF060606FFF");
        Supplier other = persistSupplier("Proveedor Eta", "GGG070707GGG");
        entityManager.clear(); // FIX: caché de primer nivel invalida resultados existsBy*

        // Act
        boolean result = supplierRepository.existsByRfcAndIdNot("FFF060606FFF", other.getId());

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void should_returnFalse_when_rfcExistsButBelongsToSameSupplier() {
        // Arrange
        Supplier s = persistSupplier("Proveedor Theta", "HHH080808HHH");
        entityManager.clear();

        // Act
        boolean result = supplierRepository.existsByRfcAndIdNot("HHH080808HHH", s.getId());

        // Assert
        assertThat(result).isFalse();
    }

    // ─────────────────────────────────────────────
    //  findByIsActiveTrue
    // ─────────────────────────────────────────────

    @Test
    void should_returnOnlyActiveSuppliers_when_mixedSuppliersPersisted() {
        // Arrange
        persistSupplier("Activo Uno", "ACT111111AC1");
        persistSupplier("Activo Dos", "ACT222222AC2");
        persistInactiveSupplier("Inactivo Uno", "INA111111IN1");
        entityManager.clear(); // Problema 4: limpiar caché antes del act

        // Act
        Page<Supplier> page = supplierRepository.findByIsActiveTrue(PageRequest.of(0, 10));

        // Assert
        assertThat(page.getContent())
                .hasSize(2)
                .allMatch(Supplier::getIsActive);
    }

    @Test
    void should_returnEmptyPage_when_noActiveSuppliersExist() {
        // Arrange
        persistInactiveSupplier("Inactivo Solo", "INA999999IN9");
        entityManager.clear();

        // Act
        Page<Supplier> page = supplierRepository.findByIsActiveTrue(PageRequest.of(0, 10));

        // Assert
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void should_respectPagination_when_multipleActiveSuppliersExist() {
        // Arrange
        for (int i = 1; i <= 5; i++) {
            persistSupplier("Proveedor Paginado " + i,
                    "PAG" + String.format("%06d", i) + "P" + i);
        }
        entityManager.clear();

        // Act
        Page<Supplier> firstPage  = supplierRepository.findByIsActiveTrue(PageRequest.of(0, 3));
        Page<Supplier> secondPage = supplierRepository.findByIsActiveTrue(PageRequest.of(1, 3));

        // Assert
        assertThat(firstPage.getContent()).hasSize(3);
        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
    }

    // ─────────────────────────────────────────────
    //  searchActive  (@Query JPQL — objetivo principal)
    // ─────────────────────────────────────────────

    @Test
    void should_findSupplierByName_when_queryMatchesPartialName() {
        // Arrange
        persistSupplier("Tecnologia Industrial SA", "TEC111111TEC");
        entityManager.clear();

        // Act
        Page<Supplier> result = supplierRepository.searchActive("Industrial", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent())
                .hasSize(1)
                .first()
                .extracting(Supplier::getName)
                .isEqualTo("Tecnologia Industrial SA");
    }

    @Test
    void should_findSupplierByContactName_when_queryMatchesContactName() {
        // Arrange
        Supplier s = new Supplier();
        s.setName("Distribuidora Norte");
        s.setRfc("DIS111111DIS");
        s.setContactName("Maria Gonzalez Ruiz"); // FIX: sin acentos — H2 no normaliza Unicode con LOWER()
        s.setIsActive(true);
        entityManager.persistAndFlush(s);
        entityManager.clear();

        // Act
        Page<Supplier> result = supplierRepository.searchActive("Gonzalez", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent())
                .hasSize(1)
                .first()
                .extracting(Supplier::getContactName)
                .isEqualTo("Maria Gonzalez Ruiz");
    }

    @Test
    void should_findSupplierCaseInsensitively_when_queryHasDifferentCase() {
        // Arrange
        // FIX: sin acentos — H2 no normaliza caracteres acentuados en comparaciones LOWER()
        persistSupplier("Equipos Electronicos", "EEL111111EEL");
        entityManager.clear();

        // Act
        Page<Supplier> upperResult = supplierRepository.searchActive("EQUIPOS", PageRequest.of(0, 10));
        Page<Supplier> lowerResult = supplierRepository.searchActive("electronicos", PageRequest.of(0, 10));

        // Assert
        assertThat(upperResult.getContent()).hasSize(1);
        assertThat(lowerResult.getContent()).hasSize(1);
    }

    @Test
    void should_excludeInactiveSuppliers_when_searchingActiveByQuery() {
        // Arrange
        // FIX: sin acentos para evitar fallo por normalización Unicode en H2
        persistSupplier("Activo Electronica", "ACE111111ACE");
        persistInactiveSupplier("Inactivo Electronica", "INE111111INE");
        entityManager.clear();

        // Act
        Page<Supplier> result = supplierRepository.searchActive("Electronica", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent())
                .hasSize(1)
                .first()
                .extracting(Supplier::getIsActive)
                .isEqualTo(true);
    }

    @Test
    void should_returnEmpty_when_noActiveSupplierMatchesQuery() {
        // Arrange
        persistSupplier("Proveedor Sin Relacion", "PSR111111PSR");
        entityManager.clear();

        // Act
        Page<Supplier> result = supplierRepository.searchActive("termino_inexistente_xyz", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void should_returnMultipleResults_when_queryMatchesSeveralActiveSuppliers() {
        // Arrange
        Supplier s1 = new Supplier();
        s1.setName("Proveedor Logistica SA");
        s1.setRfc("LOG111111LOG");
        s1.setIsActive(true); // FIX: siempre explícito
        entityManager.persistAndFlush(s1);

        Supplier s2 = new Supplier();
        s2.setName("Servicios Comerciales");
        s2.setRfc("SER222222SER");
        s2.setContactName("Pedro Logistica Torres");
        s2.setIsActive(true); // FIX: siempre explícito
        entityManager.persistAndFlush(s2);
        entityManager.clear();

        // Act
        Page<Supplier> result = supplierRepository.searchActive("Logistica", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(2);
    }
}