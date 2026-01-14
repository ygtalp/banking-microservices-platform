package com.banking.auth.repository;

import com.banking.auth.model.Role;
import com.banking.auth.model.User;
import com.banking.auth.model.UserStatus;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("User Repository Database Tests")
class UserRepositoryTest {

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
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User activeUser;
    private User lockedUser;
    private User inactiveUser;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create a sample role
        customerRole = Role.builder()
                .roleName("ROLE_CUSTOMER")
                .description("Customer role")
                .build();
        roleRepository.save(customerRole);

        // Active user
        activeUser = User.builder()
                .userId("USR-123456789012")
                .email("john.doe@example.com")
                .passwordHash("$2a$12$hashed.password.here")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+905551234567")
                .status(UserStatus.ACTIVE)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .mfaEnabled(false)
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .build();
        activeUser.addRole(customerRole);

        // Locked user
        lockedUser = User.builder()
                .userId("USR-223456789012")
                .email("jane.smith@example.com")
                .passwordHash("$2a$12$hashed.password.here")
                .firstName("Jane")
                .lastName("Smith")
                .status(UserStatus.LOCKED)
                .accountLocked(true)
                .failedLoginAttempts(5)
                .lockedAt(LocalDateTime.now().minusHours(2))
                .mfaEnabled(false)
                .build();

        // Inactive user (no recent login)
        inactiveUser = User.builder()
                .userId("USR-323456789012")
                .email("bob.inactive@example.com")
                .passwordHash("$2a$12$hashed.password.here")
                .firstName("Bob")
                .lastName("Inactive")
                .status(UserStatus.INACTIVE)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .mfaEnabled(false)
                .lastLoginAt(LocalDateTime.now().minusDays(100))
                .build();
    }

    // ==================== BASIC CRUD TESTS ====================

    @Test
    @DisplayName("Should save user successfully")
    void shouldSaveUserSuccessfully() {
        // When
        User savedUser = userRepository.save(activeUser);

        // Then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUserId()).isEqualTo("USR-123456789012");
        assertThat(savedUser.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(savedUser.getFirstName()).isEqualTo("John");
        assertThat(savedUser.getLastName()).isEqualTo("Doe");
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getAccountLocked()).isFalse();
        assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getRoles()).hasSize(1);
    }

    @Test
    @DisplayName("Should find user by id successfully")
    void shouldFindUserByIdSuccessfully() {
        // Given
        User savedUser = userRepository.save(activeUser);

        // When
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUserId()).isEqualTo("USR-123456789012");
        assertThat(foundUser.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should update user successfully")
    void shouldUpdateUserSuccessfully() {
        // Given
        User savedUser = userRepository.save(activeUser);

        // When
        savedUser.setFirstName("John Updated");
        savedUser.setStatus(UserStatus.SUSPENDED);
        User updatedUser = userRepository.save(savedUser);

        // Then
        assertThat(updatedUser.getFirstName()).isEqualTo("John Updated");
        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() {
        // Given
        User savedUser = userRepository.save(activeUser);

        // When
        userRepository.delete(savedUser);

        // Then
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertThat(foundUser).isEmpty();
    }

    // ==================== FIND BY USER_ID TESTS ====================

    @Test
    @DisplayName("Should find user by userId")
    void shouldFindUserByUserId() {
        // Given
        userRepository.save(activeUser);

        // When
        Optional<User> foundUser = userRepository.findByUserId("USR-123456789012");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(foundUser.get().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return empty when userId not found")
    void shouldReturnEmptyWhenUserIdNotFound() {
        // When
        Optional<User> foundUser = userRepository.findByUserId("USR-NOTFOUND");

        // Then
        assertThat(foundUser).isEmpty();
    }

    // ==================== FIND BY EMAIL TESTS ====================

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        userRepository.save(activeUser);

        // When
        Optional<User> foundUser = userRepository.findByEmail("john.doe@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUserId()).isEqualTo("USR-123456789012");
        assertThat(foundUser.get().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void shouldReturnEmptyWhenEmailNotFound() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("notfound@example.com");

        // Then
        assertThat(foundUser).isEmpty();
    }

    // ==================== EXISTS TESTS ====================

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckIfEmailExists() {
        // Given
        userRepository.save(activeUser);

        // When
        boolean exists = userRepository.existsByEmail("john.doe@example.com");
        boolean notExists = userRepository.existsByEmail("notfound@example.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should check if userId exists")
    void shouldCheckIfUserIdExists() {
        // Given
        userRepository.save(activeUser);

        // When
        boolean exists = userRepository.existsByUserId("USR-123456789012");
        boolean notExists = userRepository.existsByUserId("USR-NOTFOUND");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    // ==================== FIND BY STATUS TESTS ====================

    @Test
    @DisplayName("Should find users by status")
    void shouldFindUsersByStatus() {
        // Given
        userRepository.save(activeUser);
        userRepository.save(lockedUser);
        userRepository.save(inactiveUser);

        // When
        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
        List<User> lockedUsers = userRepository.findByStatus(UserStatus.LOCKED);
        List<User> inactiveUsers = userRepository.findByStatus(UserStatus.INACTIVE);

        // Then
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getEmail()).isEqualTo("john.doe@example.com");

        assertThat(lockedUsers).hasSize(1);
        assertThat(lockedUsers.get(0).getEmail()).isEqualTo("jane.smith@example.com");

        assertThat(inactiveUsers).hasSize(1);
        assertThat(inactiveUsers.get(0).getEmail()).isEqualTo("bob.inactive@example.com");
    }

    @Test
    @DisplayName("Should return empty list when no users with status")
    void shouldReturnEmptyListWhenNoUsersWithStatus() {
        // When
        List<User> suspendedUsers = userRepository.findByStatus(UserStatus.SUSPENDED);

        // Then
        assertThat(suspendedUsers).isEmpty();
    }

    // ==================== FIND BY ACCOUNT_LOCKED TESTS ====================

    @Test
    @DisplayName("Should find users by account locked status")
    void shouldFindUsersByAccountLockedStatus() {
        // Given
        userRepository.save(activeUser);
        userRepository.save(lockedUser);
        userRepository.save(inactiveUser);

        // When
        List<User> lockedUsers = userRepository.findByAccountLocked(true);
        List<User> unlockedUsers = userRepository.findByAccountLocked(false);

        // Then
        assertThat(lockedUsers).hasSize(1);
        assertThat(lockedUsers.get(0).getEmail()).isEqualTo("jane.smith@example.com");

        assertThat(unlockedUsers).hasSize(2);
    }

    // ==================== FIND LOCKED USERS SINCE TESTS ====================

    @Test
    @DisplayName("Should find locked users since a specific date")
    void shouldFindLockedUsersSinceSpecificDate() {
        // Given
        userRepository.save(activeUser);
        userRepository.save(lockedUser);

        // When
        List<User> recentlyLockedUsers = userRepository.findLockedUsersSince(LocalDateTime.now().minusHours(3));
        List<User> olderLockedUsers = userRepository.findLockedUsersSince(LocalDateTime.now().minusHours(1));

        // Then
        assertThat(recentlyLockedUsers).hasSize(1);
        assertThat(recentlyLockedUsers.get(0).getEmail()).isEqualTo("jane.smith@example.com");

        assertThat(olderLockedUsers).isEmpty();
    }

    // ==================== FIND INACTIVE USERS SINCE TESTS ====================

    @Test
    @DisplayName("Should find inactive users since a specific date")
    void shouldFindInactiveUsersSinceSpecificDate() {
        // Given
        userRepository.save(activeUser);
        userRepository.save(inactiveUser);

        // When - Find users who haven't logged in for 30 days
        List<User> inactiveUsers = userRepository.findInactiveUsersSince(LocalDateTime.now().minusDays(30));

        // Then
        assertThat(inactiveUsers).hasSize(1);
        assertThat(inactiveUsers.get(0).getEmail()).isEqualTo("bob.inactive@example.com");
    }

    @Test
    @DisplayName("Should find users with null lastLoginAt")
    void shouldFindUsersWithNullLastLoginAt() {
        // Given
        User newUser = User.builder()
                .userId("USR-424356789012")
                .email("new.user@example.com")
                .passwordHash("$2a$12$hashed")
                .firstName("New")
                .lastName("User")
                .status(UserStatus.INACTIVE)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .mfaEnabled(false)
                .lastLoginAt(null) // Never logged in
                .build();
        userRepository.save(newUser);

        // When
        List<User> inactiveUsers = userRepository.findInactiveUsersSince(LocalDateTime.now().minusDays(1));

        // Then
        assertThat(inactiveUsers).hasSize(1);
        assertThat(inactiveUsers.get(0).getEmail()).isEqualTo("new.user@example.com");
    }

    // ==================== SEARCH USERS TESTS ====================

    @Test
    @DisplayName("Should search users by first name")
    void shouldSearchUsersByFirstName() {
        // Given
        userRepository.save(activeUser);
        userRepository.save(lockedUser);

        // When
        List<User> foundUsers = userRepository.searchUsers("john");

        // Then
        assertThat(foundUsers).hasSize(1);
        assertThat(foundUsers.get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should search users by last name")
    void shouldSearchUsersByLastName() {
        // Given
        userRepository.save(activeUser);
        userRepository.save(lockedUser);

        // When
        List<User> foundUsers = userRepository.searchUsers("smith");

        // Then
        assertThat(foundUsers).hasSize(1);
        assertThat(foundUsers.get(0).getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("Should search users by email")
    void shouldSearchUsersByEmail() {
        // Given
        userRepository.save(activeUser);
        userRepository.save(lockedUser);

        // When
        List<User> foundUsers = userRepository.searchUsers("jane.smith");

        // Then
        assertThat(foundUsers).hasSize(1);
        assertThat(foundUsers.get(0).getEmail()).isEqualTo("jane.smith@example.com");
    }

    @Test
    @DisplayName("Should search users case-insensitively")
    void shouldSearchUsersCaseInsensitively() {
        // Given
        userRepository.save(activeUser);

        // When
        List<User> foundUsers1 = userRepository.searchUsers("JOHN");
        List<User> foundUsers2 = userRepository.searchUsers("john");
        List<User> foundUsers3 = userRepository.searchUsers("JoHn");

        // Then
        assertThat(foundUsers1).hasSize(1);
        assertThat(foundUsers2).hasSize(1);
        assertThat(foundUsers3).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty list when search term not found")
    void shouldReturnEmptyListWhenSearchTermNotFound() {
        // Given
        userRepository.save(activeUser);

        // When
        List<User> foundUsers = userRepository.searchUsers("nonexistent");

        // Then
        assertThat(foundUsers).isEmpty();
    }

    // ==================== COUNT TESTS ====================

    @Test
    @DisplayName("Should count users by status")
    void shouldCountUsersByStatus() {
        // Given
        userRepository.save(activeUser);
        userRepository.save(lockedUser);
        userRepository.save(inactiveUser);

        // When
        long activeCount = userRepository.countByStatus(UserStatus.ACTIVE);
        long lockedCount = userRepository.countByStatus(UserStatus.LOCKED);
        long inactiveCount = userRepository.countByStatus(UserStatus.INACTIVE);
        long suspendedCount = userRepository.countByStatus(UserStatus.SUSPENDED);

        // Then
        assertThat(activeCount).isEqualTo(1);
        assertThat(lockedCount).isEqualTo(1);
        assertThat(inactiveCount).isEqualTo(1);
        assertThat(suspendedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should count users by account locked status")
    void shouldCountUsersByAccountLockedStatus() {
        // Given
        userRepository.save(activeUser);
        userRepository.save(lockedUser);
        userRepository.save(inactiveUser);

        // When
        long lockedCount = userRepository.countByAccountLocked(true);
        long unlockedCount = userRepository.countByAccountLocked(false);

        // Then
        assertThat(lockedCount).isEqualTo(1);
        assertThat(unlockedCount).isEqualTo(2);
    }

    // ==================== UNIQUE CONSTRAINT TESTS ====================

    @Test
    @DisplayName("Should fail to save user with duplicate email")
    void shouldFailToSaveUserWithDuplicateEmail() {
        // Given
        userRepository.save(activeUser);

        User duplicateEmailUser = User.builder()
                .userId("USR-999999999999")
                .email("john.doe@example.com") // Duplicate email
                .passwordHash("$2a$12$hashed")
                .firstName("Duplicate")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .mfaEnabled(false)
                .build();

        // When & Then
        assertThatThrownBy(() -> {
            userRepository.save(duplicateEmailUser);
            userRepository.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should fail to save user with duplicate userId")
    void shouldFailToSaveUserWithDuplicateUserId() {
        // Given
        userRepository.save(activeUser);

        User duplicateUserIdUser = User.builder()
                .userId("USR-123456789012") // Duplicate userId
                .email("different@example.com")
                .passwordHash("$2a$12$hashed")
                .firstName("Duplicate")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .mfaEnabled(false)
                .build();

        // When & Then
        assertThatThrownBy(() -> {
            userRepository.save(duplicateUserIdUser);
            userRepository.flush();
        }).isInstanceOf(Exception.class);
    }

    // ==================== USER HELPER METHOD TESTS ====================

    @Test
    @DisplayName("Should increment failed attempts and lock account after 5 attempts")
    void shouldIncrementFailedAttemptsAndLockAccount() {
        // Given
        userRepository.save(activeUser);

        // When - Increment failed attempts 5 times
        for (int i = 0; i < 5; i++) {
            activeUser.incrementFailedAttempts();
        }
        User updatedUser = userRepository.save(activeUser);

        // Then
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(updatedUser.getAccountLocked()).isTrue();
        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(updatedUser.getLockedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reset failed attempts")
    void shouldResetFailedAttempts() {
        // Given
        lockedUser.setFailedLoginAttempts(5);
        userRepository.save(lockedUser);

        // When
        lockedUser.resetFailedAttempts();
        User updatedUser = userRepository.save(lockedUser);

        // Then
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should unlock user account")
    void shouldUnlockUserAccount() {
        // Given
        userRepository.save(lockedUser);

        // When
        lockedUser.unlock();
        User updatedUser = userRepository.save(lockedUser);

        // Then
        assertThat(updatedUser.getAccountLocked()).isFalse();
        assertThat(updatedUser.getLockedAt()).isNull();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    // ==================== ROLE RELATIONSHIP TESTS ====================

    @Test
    @DisplayName("Should add role to user")
    void shouldAddRoleToUser() {
        // Given
        Role adminRole = Role.builder()
                .roleName("ROLE_ADMIN")
                .description("Admin role")
                .build();
        roleRepository.save(adminRole);

        userRepository.save(activeUser);

        // When
        activeUser.addRole(adminRole);
        User updatedUser = userRepository.save(activeUser);

        // Then
        assertThat(updatedUser.getRoles()).hasSize(2);
        assertThat(updatedUser.getRoles()).extracting("roleName")
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should remove role from user")
    void shouldRemoveRoleFromUser() {
        // Given
        userRepository.save(activeUser);

        // When
        activeUser.removeRole(customerRole);
        User updatedUser = userRepository.save(activeUser);

        // Then
        assertThat(updatedUser.getRoles()).isEmpty();
    }

    // ==================== MFA TESTS ====================

    @Test
    @DisplayName("Should enable MFA for user")
    void shouldEnableMfaForUser() {
        // Given
        userRepository.save(activeUser);

        // When
        activeUser.enableMfa(com.banking.auth.model.MfaMethod.TOTP);
        User updatedUser = userRepository.save(activeUser);

        // Then
        assertThat(updatedUser.getMfaEnabled()).isTrue();
        assertThat(updatedUser.getPreferredMfaMethod()).isEqualTo(com.banking.auth.model.MfaMethod.TOTP);
        assertThat(updatedUser.isMfaEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should disable MFA for user")
    void shouldDisableMfaForUser() {
        // Given
        activeUser.enableMfa(com.banking.auth.model.MfaMethod.TOTP);
        userRepository.save(activeUser);

        // When
        activeUser.disableMfa();
        User updatedUser = userRepository.save(activeUser);

        // Then
        assertThat(updatedUser.getMfaEnabled()).isFalse();
        assertThat(updatedUser.getPreferredMfaMethod()).isNull();
        assertThat(updatedUser.isMfaEnabled()).isFalse();
    }

    // ==================== TIMESTAMP AUTO-GENERATION TESTS ====================

    @Test
    @DisplayName("Should auto-generate createdAt timestamp")
    void shouldAutoGenerateCreatedAtTimestamp() {
        // When
        User savedUser = userRepository.save(activeUser);

        // Then
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("Should auto-generate updatedAt timestamp")
    void shouldAutoGenerateUpdatedAtTimestamp() {
        // When
        User savedUser = userRepository.save(activeUser);

        // Then
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("Should update updatedAt on modification")
    void shouldUpdateUpdatedAtOnModification() throws InterruptedException {
        // Given
        User savedUser = userRepository.save(activeUser);
        LocalDateTime initialUpdatedAt = savedUser.getUpdatedAt();

        // When
        Thread.sleep(100); // Ensure time difference
        savedUser.setFirstName("John Modified");
        User updatedUser = userRepository.save(savedUser);

        // Then
        assertThat(updatedUser.getUpdatedAt()).isAfter(initialUpdatedAt);
    }
}
