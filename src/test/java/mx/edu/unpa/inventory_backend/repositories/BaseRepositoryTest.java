package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.*;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

@DataJpaTest
@ActiveProfiles("test")
public abstract class BaseRepositoryTest {

    @Autowired
    protected TestEntityManager entityManager;
    @Autowired protected JdbcTemplate jdbcTemplate;

    protected Category category;
    protected User operatorUser;
    protected Brand brand;

    @BeforeEach
    void setUpBase() {
        category     = entityManager.persistAndFlush(buildCategory("Equipos de Cómputo"));
        operatorUser = entityManager.persistAndFlush(buildUser("operador01"));
        brand        = entityManager.persistAndFlush(buildBrand("Dell"));
    }

    protected Category buildCategory(String name) {
        Category c = new Category();
        c.setName(name);
        return c;
    }

    protected User buildUser(String username) {
        return User.builder()
                .username(username)
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.OPERADOR)
                .isActive(true)
                .build();
    }

    protected Brand buildBrand(String name) {
        Brand b = new Brand();
        b.setName(name);
        b.setIsActive(true);
        return b;
    }

    protected Guardian buildGuardian(String employeeNumber, String fullName) {
        Guardian g = new Guardian();
        g.setEmployeeNumber(employeeNumber);
        g.setFullName(fullName);
        g.setIsActive(true);
        return g;
    }

    protected Location buildLocation(String name) {
        Location l = new Location();
        l.setName(name);
        return l;
    }

    protected Asset buildAsset(String inventoryNumber) {
        Asset a = new Asset();
        a.setInventoryNumber(inventoryNumber);
        a.setDescription("Bien de prueba");
        a.setCategory(category);
        a.setBrand(brand);          // siempre setear para evitar INNER JOIN implícito
        a.setEntryDate(LocalDate.now());
        a.setConditionStatus(ConditionStatus.GOOD);
        a.setLifecycleStatus(LifecycleStatus.REGISTERED);
        a.setCreatedBy(operatorUser);
        a.setUpdatedBy(operatorUser);
        return a;
    }

    protected Asset buildAssetWithStatus(String inv,
                                         ConditionStatus condition,
                                         LifecycleStatus lifecycle) {
        Asset a = buildAsset(inv);
        a.setConditionStatus(condition);
        a.setLifecycleStatus(lifecycle);
        return a;
    }
}
