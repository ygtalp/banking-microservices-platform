package com.banking.auth.service;

import com.banking.auth.dto.ApiResponse;
import com.banking.auth.dto.ChangePasswordRequest;
import com.banking.auth.dto.UserProfileResponse;
import com.banking.auth.exception.InvalidPasswordException;
import com.banking.auth.exception.UserNotFoundException;
import com.banking.auth.model.Role;
import com.banking.auth.model.User;
import com.banking.auth.model.UserStatus;
import com.banking.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder()
                .roleName("CUSTOMER")
                .description("Customer role")
                .permissions(new HashSet<>())
                .build();

        testUser = User.builder()
                .id(1L)
                .userId("USR-TEST123456")
                .email("test@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+905551234567")
                .status(UserStatus.ACTIVE)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusMonths(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("Should get user profile by userId successfully")
    void testGetUserProfile_Success() {
        // Arrange
        when(userRepository.findByUserId("USR-TEST123456")).thenReturn(Optional.of(testUser));

        // Act
        ApiResponse<UserProfileResponse> response = userService.getUserProfile("USR-TEST123456");

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("USR-TEST123456", response.getData().getUserId());
        assertEquals("test@example.com", response.getData().getEmail());
        assertEquals("John", response.getData().getFirstName());
        assertEquals("Doe", response.getData().getLastName());
        assertEquals(UserStatus.ACTIVE, response.getData().getStatus());
        assertTrue(response.getData().getRoles().contains("CUSTOMER"));

        verify(userRepository).findByUserId("USR-TEST123456");
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found by userId")
    void testGetUserProfile_UserNotFound() {
        // Arrange
        when(userRepository.findByUserId(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserProfile("USR-NOTFOUND");
        });

        verify(userRepository).findByUserId("USR-NOTFOUND");
    }

    @Test
    @DisplayName("Should get user profile by email successfully")
    void testGetUserProfileByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        ApiResponse<UserProfileResponse> response = userService.getUserProfileByEmail("test@example.com");

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("test@example.com", response.getData().getEmail());

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found by email")
    void testGetUserProfileByEmail_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserProfileByEmail("notfound@example.com");
        });
    }

    @Test
    @DisplayName("Should change password successfully")
    void testChangePassword_Success() {
        // Arrange
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPass@123")
                .newPassword("NewPass@456")
                .build();

        // Set original password hash
        String originalHash = "$2a$12$originalhashedpassword";
        testUser.setPasswordHash(originalHash);

        when(userRepository.findByUserId("USR-TEST123456")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPass@123", originalHash)).thenReturn(true);
        when(passwordEncoder.encode("NewPass@456")).thenReturn("$2a$12$newhashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        // Act
        ApiResponse<Void> response = userService.changePassword("USR-TEST123456", request);

        // Assert
        assertTrue(response.isSuccess());
        verify(passwordEncoder).matches("OldPass@123", originalHash);
        verify(passwordEncoder).encode("NewPass@456");
        verify(userRepository).save(any(User.class));
        verify(kafkaTemplate).send(eq("user-events"), any());
    }

    @Test
    @DisplayName("Should throw InvalidPasswordException when current password is incorrect")
    void testChangePassword_IncorrectCurrentPassword() {
        // Arrange
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPass@123")
                .newPassword("NewPass@456")
                .build();

        when(userRepository.findByUserId("USR-TEST123456")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPass@123", testUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidPasswordException.class, () -> {
            userService.changePassword("USR-TEST123456", request);
        });

        verify(userRepository, never()).save(any(User.class));
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("Should throw InvalidPasswordException when new password equals current password")
    void testChangePassword_SamePassword() {
        // Arrange
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("SamePass@123")
                .newPassword("SamePass@123")
                .build();

        when(userRepository.findByUserId("USR-TEST123456")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("SamePass@123", testUser.getPasswordHash())).thenReturn(true);

        // Act & Assert
        assertThrows(InvalidPasswordException.class, () -> {
            userService.changePassword("USR-TEST123456", request);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should unlock account successfully")
    void testUnlockAccount_Success() {
        // Arrange
        testUser.setAccountLocked(true);
        testUser.setLockedAt(LocalDateTime.now());
        testUser.setFailedLoginAttempts(5);

        when(userRepository.findByUserId("USR-TEST123456")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        ApiResponse<Void> response = userService.unlockAccount("USR-TEST123456");

        // Assert
        assertTrue(response.isSuccess());
        verify(userRepository).save(argThat(user ->
                !user.getAccountLocked() &&
                user.getLockedAt() == null &&
                user.getFailedLoginAttempts() == 0 &&
                user.getStatus() == UserStatus.ACTIVE
        ));
    }

    @Test
    @DisplayName("Should return success message when account is not locked")
    void testUnlockAccount_NotLocked() {
        // Arrange
        testUser.setAccountLocked(false);
        when(userRepository.findByUserId("USR-TEST123456")).thenReturn(Optional.of(testUser));

        // Act
        ApiResponse<Void> response = userService.unlockAccount("USR-TEST123456");

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Account is not locked", response.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should get all locked accounts")
    void testGetLockedAccounts_Success() {
        // Arrange
        User lockedUser1 = User.builder()
                .userId("USR-LOCKED001")
                .email("locked1@example.com")
                .firstName("Locked")
                .lastName("User1")
                .status(UserStatus.LOCKED)
                .accountLocked(true)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User lockedUser2 = User.builder()
                .userId("USR-LOCKED002")
                .email("locked2@example.com")
                .firstName("Locked")
                .lastName("User2")
                .status(UserStatus.LOCKED)
                .accountLocked(true)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByAccountLocked(true))
                .thenReturn(Arrays.asList(lockedUser1, lockedUser2));

        // Act
        ApiResponse<List<UserProfileResponse>> response = userService.getLockedAccounts();

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());

        verify(userRepository).findByAccountLocked(true);
    }

    @Test
    @DisplayName("Should return empty list when no accounts are locked")
    void testGetLockedAccounts_NoLockedAccounts() {
        // Arrange
        when(userRepository.findByAccountLocked(true)).thenReturn(Collections.emptyList());

        // Act
        ApiResponse<List<UserProfileResponse>> response = userService.getLockedAccounts();

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertTrue(response.getData().isEmpty());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when changing password for non-existent user")
    void testChangePassword_UserNotFound() {
        // Arrange
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPass@123")
                .newPassword("NewPass@456")
                .build();

        when(userRepository.findByUserId(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.changePassword("USR-NOTFOUND", request);
        });
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when unlocking non-existent account")
    void testUnlockAccount_UserNotFound() {
        // Arrange
        when(userRepository.findByUserId(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.unlockAccount("USR-NOTFOUND");
        });
    }
}
