package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Guardian;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// SIN @TestPropertySource — ya está en application-test.properties
class UserRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    /**
     * Construye un User activo con el rol dado.
     *
     * NOTA: NO se redefine buildUser(String) aquí a propósito.
     * BaseRepositoryTest ya declara buildUser(String) como protected y su
     * @BeforeEach lo llama con "operador01". Redefinir ese metodo en la
     * subclase causaría que el @BeforeEach de la base llame a ESTA versión,
     * lo que generaría un ConstraintViolationException por username duplicado
     * cada vez que un test del repositorio también use "operador01".
     *
     * Convención de este test: todos los usernames llevan el prefijo "usr_"
     * para garantizar que nunca colisionen con el "operador01" que persiste
     * la clase base antes de cada test.
     */
    private User buildTestUser(String username, UserRole role) {
        return User.builder()
                .username(username)
                .passwordHash("$2a$10$hashedpassword")
                .role(role)
                .isActive(true)
                .build();
    }

    /** Construye un User activo con rol OPERADOR (caso más común). */
    private User buildTestUser(String username) {
        return buildTestUser(username, UserRole.OPERADOR);
    }

    /** Construye un User inactivo con rol OPERADOR. */
    private User buildInactiveTestUser(String username) {
        return User.builder()
                .username(username)
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.OPERADOR)
                .isActive(false)
                .build();
    }

    /**
     * Persiste un Guardian con los campos mínimos obligatorios.
     * isActive se setea explícitamente: Hibernate 6 + H2 puede omitir
     * el inicializador de campo Java del INSERT si no está marcado como dirty.
     */
    private Guardian persistGuardian(String employeeNumber, String fullName) {
        Guardian g = new Guardian();
        g.setEmployeeNumber(employeeNumber);
        g.setFullName(fullName);
        g.setIsActive(true);
        return entityManager.persistAndFlush(g);
    }

    /** Persiste un Guardian con email (tests de búsqueda por email). */
    private Guardian persistGuardianWithEmail(String employeeNumber, String fullName, String email) {
        Guardian g = new Guardian();
        g.setEmployeeNumber(employeeNumber);
        g.setFullName(fullName);
        g.setEmail(email);
        g.setIsActive(true);
        return entityManager.persistAndFlush(g);
    }

    // ─────────────────────────────────────────────
    //  findByUsername
    // ─────────────────────────────────────────────

    @Test
    void should_returnUser_when_usernameExists() {
        // Arrange
        entityManager.persistAndFlush(buildTestUser("usr_jperez"));
        entityManager.clear();

        // Act
        Optional<User> result = userRepository.findByUsername("usr_jperez");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("usr_jperez");
    }

    @Test
    void should_returnEmpty_when_usernameDoesNotExist() {
        // Act
        Optional<User> result = userRepository.findByUsername("usr_inexistente");

        // Assert
        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    //  findByIdAndIsActiveTrue
    // ─────────────────────────────────────────────

    @Test
    void should_returnUser_when_userExistsAndIsActive() {
        // Arrange
        User user = entityManager.persistAndFlush(buildTestUser("usr_activo01"));
        entityManager.clear();

        // Act
        Optional<User> result = userRepository.findByIdAndIsActiveTrue(user.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getIsActive()).isTrue();
    }

    @Test
    void should_returnEmpty_when_userExistsButIsInactive() {
        // Arrange
        User user = entityManager.persistAndFlush(buildInactiveTestUser("usr_inactivo01"));
        entityManager.clear();

        // Act
        Optional<User> result = userRepository.findByIdAndIsActiveTrue(user.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_idDoesNotExist() {
        // Act
        Optional<User> result = userRepository.findByIdAndIsActiveTrue(999_999L);

        // Assert
        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    //  existsByUsername
    // ─────────────────────────────────────────────

    @Test
    void should_returnTrue_when_usernameAlreadyExists() {
        // Arrange
        entityManager.persistAndFlush(buildTestUser("usr_mlopez"));
        entityManager.clear(); // invalidar caché de 1er nivel para que existsBy* vaya a BD

        // Act
        boolean result = userRepository.existsByUsername("usr_mlopez");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void should_returnFalse_when_usernameDoesNotExist() {
        // Act
        boolean result = userRepository.existsByUsername("usr_inexistente");

        // Assert
        assertThat(result).isFalse();
    }

    // ─────────────────────────────────────────────
    //  existsByGuardianId
    // ─────────────────────────────────────────────

    @Test
    void should_returnTrue_when_guardianIsAlreadyLinkedToAUser() {
        // Arrange
        Guardian guardian = persistGuardian("EMP-100", "Carlos Torres");
        User user = buildTestUser("usr_ctorres");
        user.setGuardian(guardian);
        entityManager.persistAndFlush(user);
        entityManager.clear();

        // Act
        boolean result = userRepository.existsByGuardianId(guardian.getId());

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void should_returnFalse_when_guardianHasNoLinkedUser() {
        // Arrange
        Guardian guardian = persistGuardian("EMP-101", "Sin Usuario");
        entityManager.clear();

        // Act
        boolean result = userRepository.existsByGuardianId(guardian.getId());

        // Assert
        assertThat(result).isFalse();
    }

    // ─────────────────────────────────────────────
    //  findByGuardianEmployeeNumber  (@Query JPQL — INNER JOIN)
    // ─────────────────────────────────────────────

    @Test
    void should_returnUser_when_guardianEmployeeNumberMatches() {
        // Arrange
        // Problema 1: el query usa JOIN u.guardian g (INNER JOIN implícito).
        // El User DEBE tener guardian seteado o Hibernate lo excluye del resultado.
        Guardian guardian = persistGuardian("EMP-200", "Ana Garcia");
        User user = buildTestUser("usr_agarcia");
        user.setGuardian(guardian);
        entityManager.persistAndFlush(user);
        entityManager.clear();

        // Act
        Optional<User> result = userRepository.findByGuardianEmployeeNumber("EMP-200");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("usr_agarcia");
    }

    @Test
    void should_returnEmpty_when_noUserHasGuardianWithThatEmployeeNumber() {
        // Arrange — guardian existe pero sin usuario vinculado
        persistGuardian("EMP-201", "Sin Cuenta");
        entityManager.clear();

        // Act
        Optional<User> result = userRepository.findByGuardianEmployeeNumber("EMP-201");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_employeeNumberDoesNotExistAtAll() {
        // Act
        Optional<User> result = userRepository.findByGuardianEmployeeNumber("EMP-INEXISTENTE");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnCorrectUser_when_multipleUsersWithDifferentGuardians() {
        // Arrange
        Guardian g1 = persistGuardian("EMP-202", "Luis Mendez");
        Guardian g2 = persistGuardian("EMP-203", "Rosa Juarez");

        User u1 = buildTestUser("usr_lmendez");
        u1.setGuardian(g1);
        entityManager.persistAndFlush(u1);

        User u2 = buildTestUser("usr_rjuarez");
        u2.setGuardian(g2);
        entityManager.persistAndFlush(u2);
        entityManager.clear();

        // Act
        Optional<User> result = userRepository.findByGuardianEmployeeNumber("EMP-203");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("usr_rjuarez");
    }

    // ─────────────────────────────────────────────
    //  existsByGuardianEmployeeNumber  (@Query JPQL — INNER JOIN)
    // ─────────────────────────────────────────────

    @Test
    void should_returnTrue_when_userLinkedToGuardianWithThatEmployeeNumber() {
        // Arrange
        Guardian guardian = persistGuardian("EMP-300", "Pedro Rios");
        User user = buildTestUser("usr_prios");
        user.setGuardian(guardian);
        entityManager.persistAndFlush(user);
        entityManager.clear();

        // Act
        boolean result = userRepository.existsByGuardianEmployeeNumber("EMP-300");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void should_returnFalse_when_guardianExistsButHasNoLinkedUser() {
        // Arrange
        persistGuardian("EMP-301", "Sin Cuenta");
        entityManager.clear();

        // Act
        boolean result = userRepository.existsByGuardianEmployeeNumber("EMP-301");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void should_returnFalse_when_employeeNumberDoesNotExist() {
        // Act
        boolean result = userRepository.existsByGuardianEmployeeNumber("EMP-INEXISTENTE");

        // Assert
        assertThat(result).isFalse();
    }

    // ─────────────────────────────────────────────
    //  findWithFilters  (@Query JPQL — objetivo principal)
    //  LEFT JOIN: usuarios sin guardian aparecen cuando search=null.
    //
    //  IMPORTANTE — usuario base de BaseRepositoryTest:
    //  setUpBase() persiste "operador01" (rol OPERADOR, isActive=true) antes
    //  de CADA test. Los asserts que cuentan usuarios deben incluirlo en la
    //  cuenta esperada, o usar búsquedas que lo excluyan (por prefijo "usr_").
    // ─────────────────────────────────────────────

    @Test
    void should_returnAllUsers_when_allFiltersAreNull() {
        // Arrange — la base ya tiene "operador01"; agregamos 2 más → total = 3
        entityManager.persistAndFlush(buildTestUser("usr_admin01", UserRole.ADMIN));
        entityManager.persistAndFlush(buildTestUser("usr_auditor01", UserRole.AUDITOR));
        entityManager.clear();

        // Act
        Page<User> result = userRepository.findWithFilters(null, null, null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void should_returnOnlyUsersWithRole_when_roleFilterIsProvided() {
        // Arrange — "operador01" de la base ya es OPERADOR; agregamos 1 más → 2 OPERADOR total
        entityManager.persistAndFlush(buildTestUser("usr_operador02", UserRole.OPERADOR));
        entityManager.persistAndFlush(buildTestUser("usr_admin01", UserRole.ADMIN));
        entityManager.clear();

        // Act
        Page<User> result = userRepository.findWithFilters(null, UserRole.OPERADOR, null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent())
                .hasSize(2)
                .allMatch(u -> u.getRole() == UserRole.OPERADOR);
    }

    @Test
    void should_returnOnlyActiveUsers_when_isActiveTrueFilterIsProvided() {
        // Arrange — "operador01" de la base es activo; agregamos 1 activo y 1 inactivo → 2 activos
        entityManager.persistAndFlush(buildTestUser("usr_activo01"));
        entityManager.persistAndFlush(buildInactiveTestUser("usr_inactivo01"));
        entityManager.clear();

        // Act
        Page<User> result = userRepository.findWithFilters(null, null, true, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent())
                .hasSize(2)
                .allMatch(User::getIsActive);
    }

    @Test
    void should_returnOnlyInactiveUsers_when_isActiveFalseFilterIsProvided() {
        // Arrange — "operador01" de la base es activo, no interferirá en este filtro
        entityManager.persistAndFlush(buildTestUser("usr_activo01"));
        entityManager.persistAndFlush(buildInactiveTestUser("usr_inactivo01"));
        entityManager.persistAndFlush(buildInactiveTestUser("usr_inactivo02"));
        entityManager.clear();

        // Act
        Page<User> result = userRepository.findWithFilters(null, null, false, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent())
                .hasSize(2)
                .noneMatch(User::getIsActive);
    }

    @Test
    void should_findUserByUsername_when_searchMatchesUsername() {
        // Arrange — prefijo "usr_" garantiza que "operador01" de la base no coincida
        entityManager.persistAndFlush(buildTestUser("usr_jramirez"));
        entityManager.persistAndFlush(buildTestUser("usr_mlopez"));
        entityManager.clear();

        // Act
        Page<User> result = userRepository.findWithFilters("usr_jramirez", null, null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent())
                .hasSize(1)
                .first()
                .extracting(User::getUsername)
                .isEqualTo("usr_jramirez");
    }

    @Test
    void should_findUserByGuardianFullName_when_searchMatchesFullName() {
        // Arrange
        // LEFT JOIN: el usuario necesita guardian para que g.fullName sea buscable.
        // Sin acentos: H2 no normaliza Unicode en LOWER() (ver fix Supplier).
        Guardian guardian = persistGuardian("EMP-400", "Sofia Ramos");
        User user = buildTestUser("usr_sramos");
        user.setGuardian(guardian);
        entityManager.persistAndFlush(user);
        entityManager.clear();

        // Act
        Page<User> result = userRepository.findWithFilters("Sofia", null, null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("usr_sramos");
    }

    @Test
    void should_findUserByGuardianEmail_when_searchMatchesEmail() {
        // Arrange
        Guardian guardian = persistGuardianWithEmail("EMP-401", "Fernando Cruz", "fcruz@unpa.edu.mx");
        User user = buildTestUser("usr_fcruz");
        user.setGuardian(guardian);
        entityManager.persistAndFlush(user);
        entityManager.clear();

        // Act
        Page<User> result = userRepository.findWithFilters("fcruz@unpa", null, null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("usr_fcruz");
    }

    @Test
    void should_findUserByGuardianEmployeeNumber_when_searchMatchesEmployeeNumber() {
        // Arrange
        Guardian guardian = persistGuardian("EMP-402", "Daniela Vega");
        User user = buildTestUser("usr_dvega");
        user.setGuardian(guardian);
        entityManager.persistAndFlush(user);
        entityManager.clear();

        // Act — employeeNumber NO usa LOWER() en el query, búsqueda directa con LIKE
        Page<User> result = userRepository.findWithFilters("EMP-402", null, null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("usr_dvega");
    }

    @Test
    void should_includeUsersWithoutGuardian_when_searchIsNull() {
        // Arrange — LEFT JOIN: usuario sin guardian debe aparecer cuando search=null
        entityManager.persistAndFlush(buildTestUser("usr_sin_guardian"));
        entityManager.clear();

        // Act
        Page<User> result = userRepository.findWithFilters(null, null, null, PageRequest.of(0, 10));

        // Assert — al menos el usuario sin guardian está en los resultados
        assertThat(result.getContent())
                .anyMatch(u -> u.getUsername().equals("usr_sin_guardian"));
    }

    @Test
    void should_excludeUserWithoutGuardian_when_searchTermIsProvided() {
        // Arrange — usuario sin guardian no puede coincidir con búsqueda por
        // nombre/email/num. empleado (todos son campos de Guardian)
        entityManager.persistAndFlush(buildTestUser("usr_sin_guardian_b"));

        Guardian guardian = persistGuardian("EMP-403", "Con Guardian");
        User userWithGuardian = buildTestUser("usr_con_guardian");
        userWithGuardian.setGuardian(guardian);
        entityManager.persistAndFlush(userWithGuardian);
        entityManager.clear();

        // Act
        Page<User> result = userRepository.findWithFilters("Con Guardian", null, null, PageRequest.of(0, 10));

        // Assert — solo el que tiene guardian cuyo nombre coincide
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("usr_con_guardian");
    }

    @Test
    void should_applyCombinedFilters_when_roleAndIsActiveAndSearchAreAllProvided() {
        // Arrange
        // "operador01" de la base también es OPERADOR activo, pero su username
        // NO contiene "usr_op", por lo que el filtro search="usr_op" lo excluye.
        Guardian g1 = persistGuardian("EMP-500", "Operador Activo");
        User targetUser = buildTestUser("usr_op_activo", UserRole.OPERADOR);
        targetUser.setGuardian(g1);
        entityManager.persistAndFlush(targetUser);

        Guardian g2 = persistGuardian("EMP-501", "Operador Inactivo");
        User inactiveOp = buildInactiveTestUser("usr_op_inactivo");
        inactiveOp.setGuardian(g2);
        entityManager.persistAndFlush(inactiveOp);

        entityManager.persistAndFlush(buildTestUser("usr_admin_activo", UserRole.ADMIN));
        entityManager.clear();

        // Act — search por "usr_op" excluye "operador01" de la base
        Page<User> result = userRepository.findWithFilters("usr_op", UserRole.OPERADOR, true, PageRequest.of(0, 10));

        // Assert — solo el operador activo con username que contiene "usr_op"
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("usr_op_activo");
    }

    @Test
    void should_returnEmpty_when_searchTermMatchesNoUser() {
        // Arrange
        entityManager.persistAndFlush(buildTestUser("usr_usuarioX"));
        entityManager.clear();

        // Act
        Page<User> result = userRepository.findWithFilters("termino_inexistente_xyz", null, null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void should_respectPagination_when_findWithFiltersReturnsMultipleUsers() {
        // Arrange — la base ya tiene "operador01"; agregamos 6 más → 7 en total
        // Página 0 (tamaño 4): 4 usuarios. Página 1 (tamaño 4): 3 usuarios.
        for (int i = 1; i <= 6; i++) {
            entityManager.persistAndFlush(buildTestUser("usr_paginado" + i));
        }
        entityManager.clear();

        // Act
        Page<User> firstPage  = userRepository.findWithFilters(null, null, null, PageRequest.of(0, 4));
        Page<User> secondPage = userRepository.findWithFilters(null, null, null, PageRequest.of(1, 4));

        // Assert
        assertThat(firstPage.getContent()).hasSize(4);
        assertThat(secondPage.getContent()).hasSize(3);
        assertThat(firstPage.getTotalElements()).isEqualTo(7);
    }

    @Test
    void should_searchCaseInsensitively_when_usernameHasDifferentCase() {
        // Arrange — sin acentos: H2 no normaliza Unicode en LOWER()
        entityManager.persistAndFlush(buildTestUser("usr_AdminSistema"));
        entityManager.clear();

        // Act
        Page<User> lowerResult = userRepository.findWithFilters("usr_adminsistema", null, null, PageRequest.of(0, 10));
        Page<User> upperResult = userRepository.findWithFilters("USR_ADMINSISTEMA", null, null, PageRequest.of(0, 10));

        // Assert
        assertThat(lowerResult.getContent()).hasSize(1);
        assertThat(upperResult.getContent()).hasSize(1);
    }
}