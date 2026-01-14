package com.banking.auth.repository;

import com.banking.auth.model.Permission;
import com.banking.auth.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Role Repository Database Tests")
class RoleRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private Role adminRole;
    private Role customerRole;
    private Role supportRole;
    private Permission readPermission;
    private Permission writePermission;

    @BeforeEach
    void setUp() {
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // Create sample permissions
        readPermission = Permission.builder()
                .resource("accounts")
                .action("read")
                .description("Read accounts")
                .build();
        permissionRepository.save(readPermission);

        writePermission = Permission.builder()
                .resource("accounts")
                .action("write")
                .description("Write accounts")
                .build();
        permissionRepository.save(writePermission);

        // Create sample roles
        adminRole = Role.builder()
                .roleName("ROLE_ADMIN")
                .description("Administrator role with full access")
                .build();
        adminRole.addPermission(readPermission);
        adminRole.addPermission(writePermission);

        customerRole = Role.builder()
                .roleName("ROLE_CUSTOMER")
                .description("Customer role with limited access")
                .build();
        customerRole.addPermission(readPermission);

        supportRole = Role.builder()
                .roleName("ROLE_SUPPORT")
                .description("Support staff role")
                .build();
    }

    // ==================== BASIC CRUD TESTS ====================

    @Test
    @DisplayName("Should save role successfully")
    void shouldSaveRoleSuccessfully() {
        // When
        Role savedRole = roleRepository.save(adminRole);

        // Then
        assertThat(savedRole).isNotNull();
        assertThat(savedRole.getId()).isNotNull();
        assertThat(savedRole.getRoleName()).isEqualTo("ROLE_ADMIN");
        assertThat(savedRole.getDescription()).isEqualTo("Administrator role with full access");
        assertThat(savedRole.getCreatedAt()).isNotNull();
        assertThat(savedRole.getPermissions()).hasSize(2);
    }

    @Test
    @DisplayName("Should find role by id successfully")
    void shouldFindRoleByIdSuccessfully() {
        // Given
        Role savedRole = roleRepository.save(adminRole);

        // When
        Optional<Role> foundRole = roleRepository.findById(savedRole.getId());

        // Then
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getRoleName()).isEqualTo("ROLE_ADMIN");
        assertThat(foundRole.get().getDescription()).contains("Administrator");
    }

    @Test
    @DisplayName("Should update role successfully")
    void shouldUpdateRoleSuccessfully() {
        // Given
        Role savedRole = roleRepository.save(adminRole);

        // When
        savedRole.setDescription("Updated admin role description");
        Role updatedRole = roleRepository.save(savedRole);

        // Then
        assertThat(updatedRole.getDescription()).isEqualTo("Updated admin role description");
    }

    @Test
    @DisplayName("Should delete role successfully")
    void shouldDeleteRoleSuccessfully() {
        // Given
        Role savedRole = roleRepository.save(adminRole);

        // When
        roleRepository.delete(savedRole);

        // Then
        Optional<Role> foundRole = roleRepository.findById(savedRole.getId());
        assertThat(foundRole).isEmpty();
    }

    // ==================== FIND BY ROLE_NAME TESTS ====================

    @Test
    @DisplayName("Should find role by role name")
    void shouldFindRoleByRoleName() {
        // Given
        roleRepository.save(adminRole);
        roleRepository.save(customerRole);

        // When
        Optional<Role> foundAdmin = roleRepository.findByRoleName("ROLE_ADMIN");
        Optional<Role> foundCustomer = roleRepository.findByRoleName("ROLE_CUSTOMER");

        // Then
        assertThat(foundAdmin).isPresent();
        assertThat(foundAdmin.get().getDescription()).contains("Administrator");

        assertThat(foundCustomer).isPresent();
        assertThat(foundCustomer.get().getDescription()).contains("Customer");
    }

    @Test
    @DisplayName("Should return empty when role name not found")
    void shouldReturnEmptyWhenRoleNameNotFound() {
        // When
        Optional<Role> foundRole = roleRepository.findByRoleName("ROLE_NOTFOUND");

        // Then
        assertThat(foundRole).isEmpty();
    }

    // ==================== EXISTS BY ROLE_NAME TESTS ====================

    @Test
    @DisplayName("Should check if role name exists")
    void shouldCheckIfRoleNameExists() {
        // Given
        roleRepository.save(adminRole);

        // When
        boolean exists = roleRepository.existsByRoleName("ROLE_ADMIN");
        boolean notExists = roleRepository.existsByRoleName("ROLE_NOTFOUND");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    // ==================== SEARCH BY NAME TESTS ====================

    @Test
    @DisplayName("Should search roles by name pattern")
    void shouldSearchRolesByNamePattern() {
        // Given
        roleRepository.save(adminRole);
        roleRepository.save(customerRole);
        roleRepository.save(supportRole);

        // When
        List<Role> rolesWithRole = roleRepository.searchByName("ROLE");
        List<Role> rolesWithAdmin = roleRepository.searchByName("ADMIN");
        List<Role> rolesWithSupport = roleRepository.searchByName("SUPPORT");

        // Then
        assertThat(rolesWithRole).hasSize(3);
        assertThat(rolesWithAdmin).hasSize(1);
        assertThat(rolesWithAdmin.get(0).getRoleName()).isEqualTo("ROLE_ADMIN");
        assertThat(rolesWithSupport).hasSize(1);
        assertThat(rolesWithSupport.get(0).getRoleName()).isEqualTo("ROLE_SUPPORT");
    }

    @Test
    @DisplayName("Should search roles case-insensitively")
    void shouldSearchRolesCaseInsensitively() {
        // Given
        roleRepository.save(adminRole);

        // When
        List<Role> rolesUpperCase = roleRepository.searchByName("ADMIN");
        List<Role> rolesLowerCase = roleRepository.searchByName("admin");
        List<Role> rolesMixedCase = roleRepository.searchByName("AdMiN");

        // Then
        assertThat(rolesUpperCase).hasSize(1);
        assertThat(rolesLowerCase).hasSize(1);
        assertThat(rolesMixedCase).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty list when search term not found")
    void shouldReturnEmptyListWhenSearchTermNotFound() {
        // Given
        roleRepository.save(adminRole);

        // When
        List<Role> foundRoles = roleRepository.searchByName("NONEXISTENT");

        // Then
        assertThat(foundRoles).isEmpty();
    }

    // ==================== FIND BY ROLE_NAME IN TESTS ====================

    @Test
    @DisplayName("Should find multiple roles by names")
    void shouldFindMultipleRolesByNames() {
        // Given
        roleRepository.save(adminRole);
        roleRepository.save(customerRole);
        roleRepository.save(supportRole);

        // When
        Set<Role> foundRoles = roleRepository.findByRoleNameIn(
                Arrays.asList("ROLE_ADMIN", "ROLE_CUSTOMER")
        );

        // Then
        assertThat(foundRoles).hasSize(2);
        assertThat(foundRoles).extracting("roleName")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Should return empty set when none of the role names exist")
    void shouldReturnEmptySetWhenNoneOfRoleNamesExist() {
        // Given
        roleRepository.save(adminRole);

        // When
        Set<Role> foundRoles = roleRepository.findByRoleNameIn(
                Arrays.asList("ROLE_NOTFOUND1", "ROLE_NOTFOUND2")
        );

        // Then
        assertThat(foundRoles).isEmpty();
    }

    @Test
    @DisplayName("Should find only existing roles when some names don't exist")
    void shouldFindOnlyExistingRolesWhenSomeNamesDontExist() {
        // Given
        roleRepository.save(adminRole);
        roleRepository.save(customerRole);

        // When
        Set<Role> foundRoles = roleRepository.findByRoleNameIn(
                Arrays.asList("ROLE_ADMIN", "ROLE_NOTFOUND", "ROLE_CUSTOMER")
        );

        // Then
        assertThat(foundRoles).hasSize(2);
        assertThat(foundRoles).extracting("roleName")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CUSTOMER");
    }

    // ==================== UNIQUE CONSTRAINT TESTS ====================

    @Test
    @DisplayName("Should fail to save role with duplicate role name")
    void shouldFailToSaveRoleWithDuplicateRoleName() {
        // Given
        roleRepository.save(adminRole);

        Role duplicateRole = Role.builder()
                .roleName("ROLE_ADMIN") // Duplicate
                .description("Another admin role")
                .build();

        // When & Then
        assertThatThrownBy(() -> {
            roleRepository.save(duplicateRole);
            roleRepository.flush();
        }).isInstanceOf(Exception.class);
    }

    // ==================== PERMISSION RELATIONSHIP TESTS ====================

    @Test
    @DisplayName("Should add permission to role")
    void shouldAddPermissionToRole() {
        // Given
        Permission deletePermission = Permission.builder()
                .resource("accounts")
                .action("delete")
                .description("Delete accounts")
                .build();
        permissionRepository.save(deletePermission);

        roleRepository.save(adminRole);

        // When
        adminRole.addPermission(deletePermission);
        Role updatedRole = roleRepository.save(adminRole);

        // Then
        assertThat(updatedRole.getPermissions()).hasSize(3);
        assertThat(updatedRole.getPermissions()).extracting("action")
                .containsExactlyInAnyOrder("read", "write", "delete");
    }

    @Test
    @DisplayName("Should remove permission from role")
    void shouldRemovePermissionFromRole() {
        // Given
        roleRepository.save(adminRole);

        // When
        adminRole.removePermission(writePermission);
        Role updatedRole = roleRepository.save(adminRole);

        // Then
        assertThat(updatedRole.getPermissions()).hasSize(1);
        assertThat(updatedRole.getPermissions()).extracting("action")
                .containsExactly("read");
    }

    @Test
    @DisplayName("Should load role with permissions eagerly")
    void shouldLoadRoleWithPermissionsEagerly() {
        // Given
        Role savedRole = roleRepository.save(adminRole);
        roleRepository.flush();

        // Clear persistence context to ensure fresh load
        roleRepository.findAll(); // Trigger flush

        // When
        Optional<Role> foundRole = roleRepository.findByRoleName("ROLE_ADMIN");

        // Then
        assertThat(foundRole).isPresent();
        // Permissions should be loaded (not lazy)
        assertThat(foundRole.get().getPermissions()).isNotEmpty();
        assertThat(foundRole.get().getPermissions()).hasSize(2);
    }

    // ==================== TIMESTAMP AUTO-GENERATION TESTS ====================

    @Test
    @DisplayName("Should auto-generate createdAt timestamp")
    void shouldAutoGenerateCreatedAtTimestamp() {
        // When
        Role savedRole = roleRepository.save(adminRole);

        // Then
        assertThat(savedRole.getCreatedAt()).isNotNull();
    }

    // ==================== FIND ALL TESTS ====================

    @Test
    @DisplayName("Should find all roles")
    void shouldFindAllRoles() {
        // Given
        roleRepository.save(adminRole);
        roleRepository.save(customerRole);
        roleRepository.save(supportRole);

        // When
        List<Role> allRoles = roleRepository.findAll();

        // Then
        assertThat(allRoles).hasSize(3);
        assertThat(allRoles).extracting("roleName")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CUSTOMER", "ROLE_SUPPORT");
    }

    // ==================== COUNT TESTS ====================

    @Test
    @DisplayName("Should count all roles")
    void shouldCountAllRoles() {
        // Given
        roleRepository.save(adminRole);
        roleRepository.save(customerRole);
        roleRepository.save(supportRole);

        // When
        long count = roleRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }
}
