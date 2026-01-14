package com.banking.auth.repository;

import com.banking.auth.model.Permission;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Permission Repository Database Tests")
class PermissionRepositoryTest {

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
    private PermissionRepository permissionRepository;

    private Permission accountReadPermission;
    private Permission accountWritePermission;
    private Permission accountDeletePermission;
    private Permission transferReadPermission;
    private Permission transferWritePermission;

    @BeforeEach
    void setUp() {
        permissionRepository.deleteAll();

        // Account permissions
        accountReadPermission = Permission.builder()
                .resource("accounts")
                .action("read")
                .description("Read accounts")
                .build();

        accountWritePermission = Permission.builder()
                .resource("accounts")
                .action("write")
                .description("Write/update accounts")
                .build();

        accountDeletePermission = Permission.builder()
                .resource("accounts")
                .action("delete")
                .description("Delete accounts")
                .build();

        // Transfer permissions
        transferReadPermission = Permission.builder()
                .resource("transfers")
                .action("read")
                .description("Read transfers")
                .build();

        transferWritePermission = Permission.builder()
                .resource("transfers")
                .action("write")
                .description("Create/update transfers")
                .build();
    }

    // ==================== BASIC CRUD TESTS ====================

    @Test
    @DisplayName("Should save permission successfully")
    void shouldSavePermissionSuccessfully() {
        // When
        Permission savedPermission = permissionRepository.save(accountReadPermission);

        // Then
        assertThat(savedPermission).isNotNull();
        assertThat(savedPermission.getId()).isNotNull();
        assertThat(savedPermission.getResource()).isEqualTo("accounts");
        assertThat(savedPermission.getAction()).isEqualTo("read");
        assertThat(savedPermission.getDescription()).isEqualTo("Read accounts");
        assertThat(savedPermission.getCreatedAt()).isNotNull();
        assertThat(savedPermission.getPermissionString()).isEqualTo("accounts:read");
    }

    @Test
    @DisplayName("Should find permission by id successfully")
    void shouldFindPermissionByIdSuccessfully() {
        // Given
        Permission savedPermission = permissionRepository.save(accountReadPermission);

        // When
        Optional<Permission> foundPermission = permissionRepository.findById(savedPermission.getId());

        // Then
        assertThat(foundPermission).isPresent();
        assertThat(foundPermission.get().getResource()).isEqualTo("accounts");
        assertThat(foundPermission.get().getAction()).isEqualTo("read");
    }

    @Test
    @DisplayName("Should update permission successfully")
    void shouldUpdatePermissionSuccessfully() {
        // Given
        Permission savedPermission = permissionRepository.save(accountReadPermission);

        // When
        savedPermission.setDescription("Updated description - Read all accounts");
        Permission updatedPermission = permissionRepository.save(savedPermission);

        // Then
        assertThat(updatedPermission.getDescription()).isEqualTo("Updated description - Read all accounts");
    }

    @Test
    @DisplayName("Should delete permission successfully")
    void shouldDeletePermissionSuccessfully() {
        // Given
        Permission savedPermission = permissionRepository.save(accountReadPermission);

        // When
        permissionRepository.delete(savedPermission);

        // Then
        Optional<Permission> foundPermission = permissionRepository.findById(savedPermission.getId());
        assertThat(foundPermission).isEmpty();
    }

    // ==================== FIND BY RESOURCE AND ACTION TESTS ====================

    @Test
    @DisplayName("Should find permission by resource and action")
    void shouldFindPermissionByResourceAndAction() {
        // Given
        permissionRepository.save(accountReadPermission);
        permissionRepository.save(accountWritePermission);
        permissionRepository.save(transferReadPermission);

        // When
        Optional<Permission> foundPermission1 = permissionRepository
                .findByResourceAndAction("accounts", "read");
        Optional<Permission> foundPermission2 = permissionRepository
                .findByResourceAndAction("accounts", "write");
        Optional<Permission> foundPermission3 = permissionRepository
                .findByResourceAndAction("transfers", "read");

        // Then
        assertThat(foundPermission1).isPresent();
        assertThat(foundPermission1.get().getPermissionString()).isEqualTo("accounts:read");

        assertThat(foundPermission2).isPresent();
        assertThat(foundPermission2.get().getPermissionString()).isEqualTo("accounts:write");

        assertThat(foundPermission3).isPresent();
        assertThat(foundPermission3.get().getPermissionString()).isEqualTo("transfers:read");
    }

    @Test
    @DisplayName("Should return empty when resource and action combination not found")
    void shouldReturnEmptyWhenResourceAndActionNotFound() {
        // Given
        permissionRepository.save(accountReadPermission);

        // When
        Optional<Permission> foundPermission = permissionRepository
                .findByResourceAndAction("accounts", "notfound");

        // Then
        assertThat(foundPermission).isEmpty();
    }

    // ==================== FIND BY RESOURCE TESTS ====================

    @Test
    @DisplayName("Should find all permissions by resource")
    void shouldFindAllPermissionsByResource() {
        // Given
        permissionRepository.save(accountReadPermission);
        permissionRepository.save(accountWritePermission);
        permissionRepository.save(accountDeletePermission);
        permissionRepository.save(transferReadPermission);
        permissionRepository.save(transferWritePermission);

        // When
        List<Permission> accountPermissions = permissionRepository.findByResource("accounts");
        List<Permission> transferPermissions = permissionRepository.findByResource("transfers");

        // Then
        assertThat(accountPermissions).hasSize(3);
        assertThat(accountPermissions).extracting("action")
                .containsExactlyInAnyOrder("read", "write", "delete");

        assertThat(transferPermissions).hasSize(2);
        assertThat(transferPermissions).extracting("action")
                .containsExactlyInAnyOrder("read", "write");
    }

    @Test
    @DisplayName("Should return empty list when resource not found")
    void shouldReturnEmptyListWhenResourceNotFound() {
        // Given
        permissionRepository.save(accountReadPermission);

        // When
        List<Permission> permissions = permissionRepository.findByResource("notfound");

        // Then
        assertThat(permissions).isEmpty();
    }

    // ==================== FIND BY ACTION TESTS ====================

    @Test
    @DisplayName("Should find all permissions by action")
    void shouldFindAllPermissionsByAction() {
        // Given
        permissionRepository.save(accountReadPermission);
        permissionRepository.save(accountWritePermission);
        permissionRepository.save(accountDeletePermission);
        permissionRepository.save(transferReadPermission);
        permissionRepository.save(transferWritePermission);

        // When
        List<Permission> readPermissions = permissionRepository.findByAction("read");
        List<Permission> writePermissions = permissionRepository.findByAction("write");
        List<Permission> deletePermissions = permissionRepository.findByAction("delete");

        // Then
        assertThat(readPermissions).hasSize(2);
        assertThat(readPermissions).extracting("resource")
                .containsExactlyInAnyOrder("accounts", "transfers");

        assertThat(writePermissions).hasSize(2);
        assertThat(writePermissions).extracting("resource")
                .containsExactlyInAnyOrder("accounts", "transfers");

        assertThat(deletePermissions).hasSize(1);
        assertThat(deletePermissions.get(0).getResource()).isEqualTo("accounts");
    }

    @Test
    @DisplayName("Should return empty list when action not found")
    void shouldReturnEmptyListWhenActionNotFound() {
        // Given
        permissionRepository.save(accountReadPermission);

        // When
        List<Permission> permissions = permissionRepository.findByAction("notfound");

        // Then
        assertThat(permissions).isEmpty();
    }

    // ==================== EXISTS BY RESOURCE AND ACTION TESTS ====================

    @Test
    @DisplayName("Should check if permission exists by resource and action")
    void shouldCheckIfPermissionExistsByResourceAndAction() {
        // Given
        permissionRepository.save(accountReadPermission);

        // When
        boolean exists1 = permissionRepository.existsByResourceAndAction("accounts", "read");
        boolean exists2 = permissionRepository.existsByResourceAndAction("accounts", "write");
        boolean exists3 = permissionRepository.existsByResourceAndAction("transfers", "read");

        // Then
        assertThat(exists1).isTrue();
        assertThat(exists2).isFalse();
        assertThat(exists3).isFalse();
    }

    // ==================== SEARCH PERMISSIONS TESTS ====================

    @Test
    @DisplayName("Should search permissions by resource")
    void shouldSearchPermissionsByResource() {
        // Given
        permissionRepository.save(accountReadPermission);
        permissionRepository.save(accountWritePermission);
        permissionRepository.save(transferReadPermission);

        // When
        List<Permission> foundPermissions = permissionRepository.searchPermissions("accounts");

        // Then
        assertThat(foundPermissions).hasSize(2);
        assertThat(foundPermissions).allMatch(p -> p.getResource().equals("accounts"));
    }

    @Test
    @DisplayName("Should search permissions by action")
    void shouldSearchPermissionsByAction() {
        // Given
        permissionRepository.save(accountReadPermission);
        permissionRepository.save(accountWritePermission);
        permissionRepository.save(transferReadPermission);

        // When
        List<Permission> foundPermissions = permissionRepository.searchPermissions("read");

        // Then
        assertThat(foundPermissions).hasSize(2);
        assertThat(foundPermissions).allMatch(p -> p.getAction().equals("read"));
    }

    @Test
    @DisplayName("Should search permissions case-insensitively")
    void shouldSearchPermissionsCaseInsensitively() {
        // Given
        permissionRepository.save(accountReadPermission);

        // When
        List<Permission> foundPermissions1 = permissionRepository.searchPermissions("ACCOUNTS");
        List<Permission> foundPermissions2 = permissionRepository.searchPermissions("accounts");
        List<Permission> foundPermissions3 = permissionRepository.searchPermissions("AcCoUnTs");

        // Then
        assertThat(foundPermissions1).hasSize(1);
        assertThat(foundPermissions2).hasSize(1);
        assertThat(foundPermissions3).hasSize(1);
    }

    @Test
    @DisplayName("Should search permissions with partial match")
    void shouldSearchPermissionsWithPartialMatch() {
        // Given
        permissionRepository.save(accountReadPermission);
        permissionRepository.save(accountWritePermission);

        // When
        List<Permission> foundPermissions = permissionRepository.searchPermissions("acc");

        // Then
        assertThat(foundPermissions).hasSize(2);
        assertThat(foundPermissions).allMatch(p -> p.getResource().contains("acc"));
    }

    @Test
    @DisplayName("Should return empty list when search term not found")
    void shouldReturnEmptyListWhenSearchTermNotFound() {
        // Given
        permissionRepository.save(accountReadPermission);

        // When
        List<Permission> foundPermissions = permissionRepository.searchPermissions("nonexistent");

        // Then
        assertThat(foundPermissions).isEmpty();
    }

    // ==================== UNIQUE CONSTRAINT TESTS ====================

    @Test
    @DisplayName("Should fail to save permission with duplicate resource and action")
    void shouldFailToSavePermissionWithDuplicateResourceAndAction() {
        // Given
        permissionRepository.save(accountReadPermission);

        Permission duplicatePermission = Permission.builder()
                .resource("accounts")  // Same resource
                .action("read")        // Same action
                .description("Another read accounts permission")
                .build();

        // When & Then
        assertThatThrownBy(() -> {
            permissionRepository.save(duplicatePermission);
            permissionRepository.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should allow same resource with different actions")
    void shouldAllowSameResourceWithDifferentActions() {
        // Given
        permissionRepository.save(accountReadPermission);

        // When
        Permission savedWrite = permissionRepository.save(accountWritePermission);
        Permission savedDelete = permissionRepository.save(accountDeletePermission);

        // Then
        assertThat(savedWrite).isNotNull();
        assertThat(savedDelete).isNotNull();
        assertThat(permissionRepository.findByResource("accounts")).hasSize(3);
    }

    @Test
    @DisplayName("Should allow same action with different resources")
    void shouldAllowSameActionWithDifferentResources() {
        // Given
        permissionRepository.save(accountReadPermission);

        // When
        Permission savedTransferRead = permissionRepository.save(transferReadPermission);

        // Then
        assertThat(savedTransferRead).isNotNull();
        assertThat(permissionRepository.findByAction("read")).hasSize(2);
    }

    // ==================== PERMISSION STRING HELPER TESTS ====================

    @Test
    @DisplayName("Should generate correct permission string")
    void shouldGenerateCorrectPermissionString() {
        // Given
        Permission savedPermission = permissionRepository.save(accountReadPermission);

        // When
        String permissionString = savedPermission.getPermissionString();

        // Then
        assertThat(permissionString).isEqualTo("accounts:read");
    }

    @Test
    @DisplayName("Should generate permission strings for different permissions")
    void shouldGeneratePermissionStringsForDifferentPermissions() {
        // Given
        permissionRepository.save(accountReadPermission);
        permissionRepository.save(accountWritePermission);
        permissionRepository.save(transferReadPermission);

        // When
        List<Permission> allPermissions = permissionRepository.findAll();

        // Then
        assertThat(allPermissions).extracting("permissionString")
                .containsExactlyInAnyOrder("accounts:read", "accounts:write", "transfers:read");
    }

    // ==================== TIMESTAMP AUTO-GENERATION TESTS ====================

    @Test
    @DisplayName("Should auto-generate createdAt timestamp")
    void shouldAutoGenerateCreatedAtTimestamp() {
        // When
        Permission savedPermission = permissionRepository.save(accountReadPermission);

        // Then
        assertThat(savedPermission.getCreatedAt()).isNotNull();
    }

    // ==================== FIND ALL TESTS ====================

    @Test
    @DisplayName("Should find all permissions")
    void shouldFindAllPermissions() {
        // Given
        permissionRepository.save(accountReadPermission);
        permissionRepository.save(accountWritePermission);
        permissionRepository.save(accountDeletePermission);
        permissionRepository.save(transferReadPermission);
        permissionRepository.save(transferWritePermission);

        // When
        List<Permission> allPermissions = permissionRepository.findAll();

        // Then
        assertThat(allPermissions).hasSize(5);
    }

    // ==================== COUNT TESTS ====================

    @Test
    @DisplayName("Should count all permissions")
    void shouldCountAllPermissions() {
        // Given
        permissionRepository.save(accountReadPermission);
        permissionRepository.save(accountWritePermission);
        permissionRepository.save(transferReadPermission);

        // When
        long count = permissionRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }
}
